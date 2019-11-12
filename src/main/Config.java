package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
/**
 * This class will read the Common Config parameters and makes it
 * available to use it across complete application.
 */
public final class Config {
	public static int NumberOfPreferredNeighbors;
	public static int UnchokingInterval;
	public static int OptimisticUnchokingInterval;
	public static String FileName;
	public static int FileSize;
	public static int PieceSize;
	public static int BitSetSize;

	public static void initialize() throws IOException {
		String st;
		int i1;

		try {
			BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));
			while ((st = in.readLine()) != null) {
				st = st.trim();
				if ((st.length() <= 0) || (st.startsWith("#"))) {
					continue;
				}
				String[] tokens = st.split("\\s+");
				switch (tokens[0]) {
				case "NumberOfPreferredNeighbors":
					NumberOfPreferredNeighbors = Integer.parseInt(tokens[1]);
					break;
				case "UnchokingInterval":
					UnchokingInterval = Integer.parseInt(tokens[1]);
					break;
				case "OptimisticUnchokingInterval":
					OptimisticUnchokingInterval = Integer.parseInt(tokens[1]);
					break;
				case "FileName":
					FileName = tokens[1];
					break;
				case "FileSize":
					FileSize = Integer.parseInt(tokens[1]);
					break;
				case "PieceSize":
					PieceSize = Integer.parseInt(tokens[1]);
				}
			}
			BitSetSize = (int) Math.ceil(FileSize / PieceSize);
			in.close();
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
	}
}