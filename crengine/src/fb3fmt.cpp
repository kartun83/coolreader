/**
    CoolReader Engine

    fb3fmt.cpp: FB3 support implementation.

    (c) Konstantin Potapov <pkbo@users.sourceforge.net>, 2019-2020
    This source code is distributed under the terms of
    GNU General Public License.

    See LICENSE file for details.
*/

#include "../include/fb3fmt.h"
#include "../include/lvtinydom.h"
#include "../include/fb2def.h"
#include "../include/lvstreamutils.h"
#include "../include/lvxmlparser.h"
#include "../include/crlog.h"

static const lChar32 * const fb3_BodyContentType = U"application/fb3-body+xml";
static const lChar32 * const fb3_DescriptionContentType = U"application/fb3-description+xml";
static const lChar32 * const fb3_CoverRelationship = U"http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail";
static const lChar32 * const fb3_ImageRelationship = U"http://www.fictionbook.org/FictionBook3/relationships/image";

bool DetectFb3Format( LVStreamRef stream )
{
    LVContainerRef m_arc = LVOpenArchieve( stream );
    if ( m_arc.isNull() )
        return false; // not a ZIP archive

    OpcPackage package(m_arc);

    return package.partExist(package.getContentPartName(fb3_BodyContentType));
}

class fb3DomWriter : public LVXMLParserCallback
{
private:
    fb3ImportContext *m_context;
    ldomDocumentWriter *m_parent;
    bool m_checkRole;
protected:
    void writeDescription();
public:
    /// constructor
    fb3DomWriter(ldomDocumentWriter * parent, fb3ImportContext *importContext ) :
        m_context(importContext), m_parent(parent), m_checkRole(false)
    {
    }
    // LVXMLParserCallback interface
public:
    ldomNode *OnTagOpen(const lChar32 *nsname, const lChar32 *tagname);
    /// called on closing tag
    void OnTagClose( const lChar32 * nsname, const lChar32 * tagname, bool self_closing_tag=false );
    void OnTagBody();
    void OnAttribute(const lChar32 *nsname, const lChar32 *attrname, const lChar32 *attrvalue);

    lUInt32 getFlags() { return m_parent->getFlags(); }
    void setFlags(lUInt32 flags) { m_parent->setFlags(flags); }
    void OnEncoding(const lChar32 *name, const lChar32 *table) { m_parent->OnEncoding(name, table); }
    void OnStart(LVFileFormatParser *parser) { m_parent->OnStart(parser); }
    void OnStop() { m_parent->OnStop(); }
    void OnText(const lChar32 *text, int len, lUInt32 flags) {  m_parent->OnText(text, len, flags); }
    bool OnBlob(lString32 name, const lUInt8 *data, int size) { return m_parent->OnBlob(name, data, size); }
    void OnDocProperty(const char *name, lString8 value)  { m_parent->OnDocProperty(name, value);  }
};

bool ImportFb3Document( LVStreamRef stream, ldomDocument * doc, LVDocViewCallback * progressCallback, CacheLoadingCallback * formatCallback )
{
    LVContainerRef arc = LVOpenArchieve( stream );
    if ( arc.isNull() )
        return false; // not a ZIP archive

    OpcPackage package(arc);

    fb3ImportContext context(&package);

    doc->setContainer(arc);

    package.readCoreProperties(doc->getProps());

    ldomDocument * descDoc = context.getDescription();

    if ( descDoc ) {
        lString32 language = descDoc->textFromXPath( cs32("fb3-description/lang") );
        doc->getProps()->setString(DOC_PROP_LANGUAGE, language);
    } else {
        CRLog::error("Couldn't parse description doc");
    }

#if BUILD_LITE!=1
    if ( doc->openFromCache(formatCallback) ) {
        if ( progressCallback ) {
            progressCallback->OnLoadFileEnd( );
        }
        return true;
    }
#endif

    LVStreamRef bookStream = context.openBook();
    if ( bookStream.isNull() ) {
        CRLog::error("Couldn't read a book");
        return false;
    }

    ldomDocumentWriter writer(doc);
    fb3DomWriter fb3Writer(&writer, &context);
    LVFileFormatParser * parser = new LVXMLParser(bookStream, &fb3Writer);

    bool ret = parser->Parse();
    delete parser;

    if ( !ret ) {
        CRLog::error("Couldn't parse a book");
    }

    if ( progressCallback ) {
        progressCallback->OnLoadFileEnd( );
        doc->compact();
        doc->dumpStatistics();
    }

    return ret;
}

fb3ImportContext::fb3ImportContext(OpcPackage *package) : m_package(package), m_descDoc(NULL)
{
}

fb3ImportContext::~fb3ImportContext()
{
    if(m_descDoc)
        delete  m_descDoc;
}

lString32 fb3ImportContext::geImageTarget(const lString32 relationId) {
    return m_bookPart->getRelatedPartName(fb3_ImageRelationship, relationId);
}

LVStreamRef fb3ImportContext::openBook() {
    m_bookPart = m_package->getContentPart(fb3_BodyContentType);
    m_coverImage = m_package->getRelatedPartName(fb3_CoverRelationship);
    return m_bookPart->open();
}

ldomDocument *fb3ImportContext::getDescription()
{
    if( !m_descDoc ) {
        LVStreamRef descStream = m_package->openContentPart(fb3_DescriptionContentType);

        if ( !descStream.isNull() ) {
            m_descDoc = LVParseXMLStream( descStream );
        }
    }
    return m_descDoc;
}

LVStreamRef GetFb3Coverpage(LVContainerRef arc)
{
    if ( arc.isNull() )
        return LVStreamRef(); // not a ZIP archive

    OpcPackage package(arc);
    return package.open(package.getRelatedPartName(fb3_CoverRelationship));
}

void fb3DomWriter::writeDescription()
{
    //TODO extended FB3 description
    m_parent->OnTagOpenNoAttr( NULL, U"description" );
    m_parent->OnTagOpenNoAttr( NULL, U"title-info" );
    m_parent->OnTagOpenNoAttr( NULL, U"book-title" );
    m_parent->OnTagClose( NULL, U"book-title" );
    if ( !m_context->m_coverImage.empty() ) {
        m_parent->OnTagOpenNoAttr( NULL, U"coverpage" );
        m_parent->OnTagOpen(NULL, U"image");
        m_parent->OnAttribute(U"l", U"href", m_context->m_coverImage.c_str());
        m_parent->OnTagClose( NULL, U"image", true );
        m_parent->OnTagClose( NULL, U"coverpage" );
    }
    m_parent->OnTagClose( NULL, U"title-info" );
    m_parent->OnTagClose( NULL, U"description" );
}

ldomNode *fb3DomWriter::OnTagOpen(const lChar32 *nsname, const lChar32 *tagname)
{
    if( !lStr_cmp(tagname, "fb3-body") ) {
        m_parent->OnTagOpenNoAttr(NULL, U"FictionBook");
        writeDescription();
        tagname = U"body";
    } else if ( !lStr_cmp(tagname, "notes" )) {
         m_parent->OnTagClose(NULL, U"body");
         ldomNode *footnotesBody = m_parent->OnTagOpen(NULL,U"body");
         m_parent->OnAttribute(NULL, U"name", U"notes");
         m_parent->OnTagBody();
         return footnotesBody;
    } else if( !lStr_cmp(tagname, "notebody") ) {
        tagname = U"section";
    } else if( !lStr_cmp(tagname, "note") ) {
        m_checkRole = true;
        return m_parent->OnTagOpen(nsname, U"a");
    }
    return m_parent->OnTagOpen(nsname, tagname);
}

void fb3DomWriter::OnTagClose(const lChar32 *nsname, const lChar32 *tagname, bool self_closing_tag)
{
    if ( !lStr_cmp(tagname, "fb3-body") ) {
        m_parent->OnTagClose(NULL, U"body");
        tagname = U"FictionBook";
    } else if ( !lStr_cmp(tagname, "notebody") ) {
        tagname = U"section";
    } else if( !lStr_cmp(tagname, "note") ) {
        tagname = U"a";
    } else if ( !lStr_cmp(tagname, "notes" )) {
        tagname = U"body";
    }
    m_parent->OnTagClose(nsname, tagname, self_closing_tag);
}

void fb3DomWriter::OnTagBody()
{
    m_checkRole = false;
    m_parent->OnTagBody();
}

void fb3DomWriter::OnAttribute(const lChar32 *nsname, const lChar32 *attrname, const lChar32 *attrvalue)
{
    bool pass = true;

    if( !lStr_cmp(attrname, "href") ) {
        lString32 href(attrvalue);

        if ( href.pos(":") == -1 && href[0] != '#') {
            href = cs32("#") + href;
            m_parent->OnAttribute(nsname, attrname, href.c_str());
            pass = false;
        }
    } else if ( m_checkRole && !lStr_cmp(attrname, "role") ) {
        if( !lStr_cmp(attrvalue, "footnote") )
            m_parent->OnAttribute(NULL, U"type", U"note");
        else
            m_parent->OnAttribute(NULL, U"type", U"comment");
    } else if ( !lStr_cmp(attrname, "src") ) {
        lString32 target = m_context->geImageTarget(attrvalue);
        if( !target.empty() ) {
            m_parent->OnAttribute(nsname, attrname, target.c_str());
            pass = false;
        }
    }
    if ( pass) {
        m_parent->OnAttribute(nsname, attrname, attrvalue);
    }
}
