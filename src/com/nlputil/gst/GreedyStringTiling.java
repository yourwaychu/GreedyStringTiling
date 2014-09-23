package com.nlputil.gst;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

public class GreedyStringTiling {

	public static ArrayList<MatchVals> tiles = new ArrayList<MatchVals>();
	public static ArrayList<Queue<MatchVals>> matchList = new ArrayList<Queue<MatchVals>>();

	/**
	 * This method runs a comparison on the two given strings s1 and s2
	 * returning a PlagResult object containing the similarity value, the
	 * similarities as list of tiles and a boolean value indicating suspected
	 * plagiarism.
	 * 
	 * Input: s1 and s2 : normalized Strings mML : minimumMatchingLength
	 * threshold : a single value between 0 and 1 Output: PlagResult
	 * 
	 * @param s1
	 * @param s2
	 * @param mML
	 * @param threshold
	 * @return
	 */
	public static PlagResult run(String s1, String s2, int mML, float threshold) {
		if (mML < 1)
			System.err
					.println("OutOfRangeError: minimum Matching Length mML needs to be greater than 0");
		if (!((0 <= threshold) && (threshold <= 1)))
			System.err
					.println("OutOfRangeError: treshold t needs to be 0<=t<=1");
		if (s1.isEmpty() || s2.isEmpty())
			System.err
					.println("NoValidArgumentError: input must be of type string not None");
		if (s1.equals("") || s2.equals(""))
			System.err
					.println("NoValidArgumentError: input must be of type string not None");

		// Compute Tiles
		tiles = RKR_GST(s1, s2, mML, 20);

		// Compute Similarity
		SimVal simResult = SimilarityCalculator.calcSimilarity(
				Arrays.asList(s1.split("[\\s+|\\W+]")),
				Arrays.asList(s2.split("\\s+|\\W+")), tiles, threshold);
		float similarity = simResult.similarity;
		if (similarity > 1)
			similarity = 1;

		// Create Plagiarism result and set attributes
		PlagResult result = new PlagResult(0, 0);
		result.setIdentifier(createKRHashValue(s1), createKRHashValue(s2));
		result.setTiles(tiles);
		result.setSimilarity(similarity);
		result.setSuspectedPlagiarism(simResult.suspPlag);

		System.out.println("Identifiers: " + result.getIdentifier().id1 + ":"
				+ result.getIdentifier().id2);
		System.out.println("Similarity: " + result.getSimilarity());
		System.out.print("Plagiriasm tiles: ");
		for (MatchVals tiles : result.getTiles()) {
			System.out.print("(" + tiles.patternPostion + ",");
			System.out.print(tiles.textPosition + ",");
			System.out.print(tiles.length + ")");
		}
		System.out.println("\nSuspected Plagirism: "
				+ result.suspectedPlagiarism);

		return result;
	}

	/**
	 * Computes Running-Karp-Rabin-Greedy-String-Tiling.
	 * 
	 * P pattern string T text string
	 * 
	 * More Informations can be found here: "String Similarity via Greedy String
	 * Tiling and Running Karp-Rabin Matching"
	 * http://www.pam1.bcs.uwa.edu.au/~michaelw/ftp/doc/RKR_GST.ps "YAP3:
	 * Improved Detection of Similarities in Computer Program and other Texts"
	 * http://www.pam1.bcs.uwa.edu.au/~michaelw/ftp/doc/yap3.ps
	 * 
	 * @author arunjayapal
	 * @param Pattern
	 *            string
	 * @param Text
	 *            String
	 * @param minimal
	 *            Matching length value
	 * @param Initialize
	 *            search size
	 * @return tiles
	 */
	public static ArrayList<MatchVals> RKR_GST(String P, String T,
			int minimalMatchingLength, int initsearchSize) {
		if (minimalMatchingLength < 1)
			minimalMatchingLength = 3;

		if (initsearchSize < 5)
			initsearchSize = 20;

		int s = 0;
		// String[] PList = P.split("[\\s+|\\W+]");
		// String[] TList = T.split("[\\s+|\\W+]");
		String[] PList = P.split("");
		String[] TList = T.split("");

		s = initsearchSize;
		boolean stop = false;

		while (!stop) {
			// Lmax is size of largest maximal-matches from this scan
			int Lmax = scanpattern(s, PList, TList);
			// if very long string no tiles marked. Iterate with larger s
			if (Lmax > 2 * s)
				s = Lmax;
			else {
				markStrings(s, PList, TList);
				if (s > (2 * minimalMatchingLength))
					s = s / 2;
				else if (s > minimalMatchingLength)
					s = minimalMatchingLength;
				else
					stop = true;
			}
		}
		return tiles;
	}

	/**
	 * Scans the pattern and text string lists for matches.
	 * 
	 * If a match is found that is twice as big as the search length s that size
	 * is returned, to be used to restart the scanpattern with it. All matches
	 * found are stored in a list of matches in queues.
	 * 
	 * @param s
	 * @param P
	 * @param T
	 * @return Longest maximum match
	 */
	public static int scanpattern(int s, String[] P, String[] T) {

		int longestMaxMatch = 0;
		Queue<MatchVals> queue = new LinkedList<MatchVals>();
		GSTHashTable hashtable = new GSTHashTable();
		/**
		 * Starting at the first unmarked token in T for each unmarked Tt do if
		 * distance to next tile <= s then advance t to first unmarked token
		 * after next tile else create the KR-hash value for substring Tt to
		 * Tt+s-1 and add to hashtable
		 */
		int t = 0;
		boolean noNextTile = false;
		int h;
		while (t < T.length) {
			if (isMarked(T[t])) {
				t = t + 1;
				continue;
			}

			int dist;
			Integer d = distToNextTile(t, T);
			if (d != null)
				dist = d.intValue();
			else {
				dist = T.length - t;
				noNextTile = true;
			}
			// int dist = distToNextTile(t, T);
			// No next tile found

			if (dist < s) {
				if (noNextTile)
					t = T.length;
				else {
					t = jumpToNextUnmarkedTokenAfterTile(t, dist, T);
				}
			} else {
				StringBuilder sb = new StringBuilder();

				for (int i = t; i <= t + s - 1; i++)
					sb.append(T[i]);
				String substring = sb.toString();
				h = createKRHashValue(substring);
				hashtable.add(h, t);
				t = t + 1;
			}
		}

		/**
		 * Starting at the first unmarked token of P for each unmarked Pp do if
		 * distance to next tile <= s then advance p to first unmarked token
		 * after next tile else create the KR hash-value for substring Pp to
		 * Pp+s-1 check hashtable for hash of KR hash-value for each hash-table
		 * entry with equal hashed KR hash-value do if for all j from 0 to s-1,
		 * Pp+ j = Tt+ j then k: = s while Pp+k = Tt+k AND unmarked(Pp+k) AND
		 * unmarked(Tt+k) do k := k + 1 if k > 2 *s then return(k) else record
		 * new maximal-match
		 */
		noNextTile = false;
		int p = 0;
		while (p < P.length) {
			if (isMarked(P[p])) {
				p = p + 1;
				continue;
			}

			int dist;
			Integer d = distToNextTile(p, P);
			if (d != null)
				dist = d.intValue();
			else {
				dist = P.length - p;
				noNextTile = true;
			}

			if (dist < s) {
				if (noNextTile)
					p = P.length;
				else {
					p = jumpToNextUnmarkedTokenAfterTile(p, dist, P);
				}
			} else {
				StringBuilder sb = new StringBuilder();
				for (int i = p; i <= p + s - 1; i++) {
					sb.append(P[i]);
				}
				String substring = sb.toString();
				h = createKRHashValue(substring);
				ArrayList<Integer> values = hashtable.get(h);
				if (values != null) {
					for (Integer val : values) {
						StringBuilder newsb = new StringBuilder();
						for (int i = val; i <= val + s - 1; i++) {
							newsb.append(T[i]);
						}
						if (newsb.toString().equals(substring)) {
							t = val;
							int k = s;

							while (p + k < P.length && t + k < T.length
									&& P[p + k].equals(T[t + k])
									&& isUnmarked(P[p + k])
									&& isUnmarked(T[t + k]))
								k = k + 1;

							if (k > 2 * s)
								return k;
							else {
								if (longestMaxMatch < s)
									longestMaxMatch = s;
								MatchVals mv = new MatchVals(p, t, k);
								queue.add(mv);
							}
						}
					}
				}
				p += 1;
			}

		}
		if (!queue.isEmpty()) {
			matchList.add(queue);
		}
		return longestMaxMatch;
	}

	private static void markStrings(int s, String[] P, String[] T) {
		for (Queue<MatchVals> queue : matchList) {
			while (!queue.isEmpty()) {
				MatchVals match = queue.poll();
				if (!isOccluded(match, tiles)) {
					for (int j = 0; j < match.length; j++) {
						P[match.patternPostion + j] = markToken(P[match.patternPostion
								+ j]);
						T[match.textPosition + j] = markToken(T[match.textPosition
								+ j]);
					}
					tiles.add(match);
				}
			}
		}
		matchList = new ArrayList<Queue<MatchVals>>();
	}

	/**
	 * Creates a Karp-Rabin Hash Value for the given substring and returns it.
	 * 
	 * Based on: http://www-igm.univ-mlv.fr/~lecroq/string/node5.html
	 * 
	 * @param substring
	 * @return hash value for any given string
	 */

	private static int createKRHashValue(String substring) {
		int hashValue = 0;
		for (int i = 0; i < substring.length(); i++)
			hashValue = ((hashValue << 1) + (int) substring.charAt(i));
		return hashValue;
	}

	/**
	 * If string s is unmarked returns True otherwise False.
	 * 
	 * @param string
	 * @return true or false (i.e., whether marked or unmarked)
	 */
	private static boolean isUnmarked(String string) {
		if (string.length() > 0 && string.charAt(0) != '*')
			return true;
		else
			return false;
	}

	private static boolean isMarked(String string) {
		return (!isUnmarked(string));
	}

	private static String markToken(String string) {
		StringBuilder sb = new StringBuilder();
		sb.append("*");
		sb.append(string);
		return sb.toString();
	}

	/**
	 * Returns true if the match is already occluded by another match in the
	 * tiles list.
	 * 
	 * "Note that "not occluded" is taken to mean that none of the tokens Pp to
	 * Pp+maxmatch-1 and Tt to Tt+maxmatch-1 has been marked during the creation
	 * of an earlier tile. However, given that smaller tiles cannot be created
	 * before larger ones, it suffices that only the ends of each new putative
	 * tile be testet for occlusion, rather than the whole maxmimal match." [
	 * "String Similarity via Greedy String Tiling and Running Karp-Rabin Matching"
	 * http://www.pam1.bcs.uwa.edu.au/~michaelw/ftp/doc/RKR_GST.ps]
	 * 
	 * @param match
	 * @param tiles2
	 * @return true or false
	 */
	private static boolean isOccluded(MatchVals match,
			ArrayList<MatchVals> tiles) {
		if (tiles.equals(null) || tiles == null || tiles.size() == 0)
			return false;
		for (MatchVals matches : tiles) {
			if ((matches.patternPostion + matches.length == match.patternPostion
					+ match.length)
					&& (matches.textPosition + matches.length == match.textPosition
							+ match.length))
				return true;
		}
		return false;
	}

	/**
	 * Returns distance to next tile, i.e. to next marked token. If not tile was
	 * found, it returns None.
	 * 
	 * case 1: there is a next tile -> pos + dist = first marked token -> return
	 * dist case 2: there is no next tile -> pos + dist = len(stringList) ->
	 * return None dist is also number of unmarked token 'til next tile
	 * 
	 * @param p
	 * @param p2
	 * @return distance to next tile
	 */
	private static Integer distToNextTile(int pos, String[] stringList) {
		if (pos == stringList.length)
			return null;
		int dist = 0;
		while (pos + dist + 1 < stringList.length
				&& isUnmarked(stringList[pos + dist + 1]))
			dist += 1;
		if (pos + dist + 1 == stringList.length)
			return null;
		return dist + 1;
	}

	/**
	 * Returns the first postion of an unmarked token after the next tile.
	 * 
	 * case 1: -> normal case -> tile exists -> there is an unmarked token after
	 * the tile case 2: -> tile exists -> but NO unmarked token after the tile
	 * case 3: -> NO tile exists
	 * 
	 * @param pos
	 * @param stringList
	 * @return the position to jump to the next unmarked token after tile
	 */
	private static int jumpToNextUnmarkedTokenAfterTile(int pos, int dist,
			String[] stringList) {
		pos = pos + dist;
		if (pos > stringList.length - 1)
			return pos;
		while (pos + 1 < stringList.length && (isMarked(stringList[pos + 1])))
			pos = pos + 1;
		return pos + 1;
	}

	public static void showSortedTiles(String s1, String s2, int mML) {
		if (mML < 1) {
			System.err
					.println("OutOfRangeError: minimum Matching Length mML needs to be greater than 0");
			return;
		}
		if (s1 == null || s2 == null) {
			System.err
					.println("NoValidArgumentError: input must be of type string not None");
			return;
		}
		if (s1.isEmpty() || s2.isEmpty()) {
			System.err.println("NoValidArgumentError: input string is empty");
			return;
		}

		// Compute Tiles
		tiles = RKR_GST(s1, s2, mML, 20);

		Collections.sort(tiles, new Comparator<MatchVals>() {
			@Override
			public int compare(MatchVals mv1, MatchVals mv2) {
				return mv2.length - mv1.length;
			}
		});
		// Show Tiles
		System.out.print("Tiles: \n");
		for (MatchVals tile : tiles) {
			String pattern = s1.substring(tile.patternPostion - 1,
					tile.patternPostion + tile.length - 1);
			System.out.println(pattern);
		}
	}

	private static String readFile(String filePath) {
		BufferedReader br = null;
		String content = "";
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(filePath));
			while ((sCurrentLine = br.readLine()) != null) {
				content += sCurrentLine;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
				return null;
			}
		}
		return content;
	}

	public static void main(String args[]) {
		// run("with Hash table entries Hash table entries has Arun name is here, Arun name is here with Hash table entries Arun how is arun",
		// "Hash table entries has Arun name is here, Arun name is here with Hash table entries Arun how is arun Arun name is here with Hash table entries",
		// 2, (float) 0.5);

		String a = "1234";
		String[] v = a.split("");
		for (String s : v) {
			System.out.println(s);
		}
		if (args.length != 3) {
			System.err
					.println("Wrong Format!! Usage: java -jar RKR_GST.jar filePath1 filePath2 minMatchLength");
			return;
		}
		String s1 = readFile(args[0]);
		String s2 = readFile(args[1]);
		int mML = Integer.valueOf(args[2]);
		showSortedTiles(s1, s2, mML);

	}
}
