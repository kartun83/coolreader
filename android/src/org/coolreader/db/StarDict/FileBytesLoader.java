/*
 * A simple tool converting stardict database format to SQLite.
 * Copyright (C) 2015, Nguyễn Anh Tuấn
 * Email: anhtuanbk57@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.coolreader.db.StarDict;

import org.coolreader.utils.StrUtils;

import java.io.*;

public abstract class FileBytesLoader {
    protected int bufferPos = 0;
    protected Long fileLength;
    protected FileInputStream fileIn;
    protected byte[] buffer = new byte[524288];
    protected int bytesCount;
    protected boolean finished = false;
    protected byte thisByte = 0;

    public static int byteArrayToInt(byte[] bytesToConvert) {
        byte[] bytes = bytesToConvert;

        if (bytesToConvert.length < 4) {
            bytes = new byte[4];
            System.arraycopy(bytesToConvert, 0, bytes, 0, bytesToConvert.length);
        }

        return (bytes[0] & 0xff) << 24 |
                (bytes[1] & 0xff) << 16 |
                (bytes[2] & 0xff) << 8 |
                (bytes[3] & 0xff);
    }

    public FileBytesLoader(File file) {
        fileLength = file.length();
        try {
            fileIn = new FileInputStream(file);
            readNextBuffer();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected void readNextBuffer() {
        try {
            if (fileIn.available() != 0) {
                bytesCount = fileIn.read(buffer);
            } else
                bytesCount = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void closeFile() {
        try {
            fileIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected byte getNextByte() {
        if (bufferPos >= bytesCount) {
            bufferPos = 0;
            readNextBuffer();
            if (bytesCount == 0) {
                finished = true;
                return StrUtils.UTF8_END_BYTE;
            }
        }
        bufferPos++;
        thisByte = buffer[bufferPos - 1];
        return thisByte;
    }

}
