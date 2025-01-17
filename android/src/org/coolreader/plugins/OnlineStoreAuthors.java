package org.coolreader.plugins;

import java.util.ArrayList;
import java.util.Collections;

import org.coolreader.utils.Utils;


public class OnlineStoreAuthors implements AsyncResponse {
	private ArrayList<OnlineStoreAuthor> list = new ArrayList<>();
	public void add(OnlineStoreAuthor author) {
		list.add(author);
	}
	public int size() {
		return list.size();
	}
	public OnlineStoreAuthor get(int index) {
		return list.get(index);
	}
	public void sortByName() {
		Collections.sort(list, (lhs, rhs) -> Utils.cmp(lhs.lastName, rhs.lastName));
	}
}