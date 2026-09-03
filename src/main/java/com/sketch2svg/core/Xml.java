package com.sketch2svg.core;

// XML helpers for building well-formed output
public final class Xml {

	private Xml() {}

	// Escape characters that are not allowed raw in text content or attribute values
	public static String escape(String s) {
		if (s == null)
			return "";
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '&' -> sb.append("&amp;");
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '"' -> sb.append("&quot;");
				case '\'' -> sb.append("&apos;");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}
}
