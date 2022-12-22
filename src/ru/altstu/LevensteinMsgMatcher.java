package ru.altstu;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class LevensteinMsgMatcher implements IMsgMatcher {

    List<String> messages = new LinkedList<String>();

    private int ComputeLevenshteinDistance(String s, String t) {
        int n = s.length();
        int m = t.length();
        int[][] d = new int[n + 1][m + 1];
        // Step 1
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        // Step 2
        for (int i = 0; i <= n; d[i][0] = i++);
        for (int j = 0; j <= m; d[0][j] = j++);
        // Step 3
        for (int i = 1; i <= n; i++) {
            //Step 4
            for (int j = 1; j <= m; j++) {
                // Step 5
                int cost = (t.charAt(j - 1) == s.charAt(i - 1)) ? 0 : 1;
                // Step 6
                d[i][j] = Math.min(
                        Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                        d[i - 1][j - 1] + cost);
            }
        }
        // Step 7
        return d[n][m];
    }


    @Override
    public String closestMessage(String newMsg) {
        int min = Integer.MAX_VALUE;
        String strMin = "";
        ListIterator<String> listIterator = messages.listIterator();
        while (listIterator.hasNext()) {
            String other = listIterator.next();
            int dst = ComputeLevenshteinDistance(newMsg, other);
            if (!other.equals(newMsg) && dst < min) {
                min = dst;
                strMin = other;
            }
        }
        return strMin;
    }

    @Override
    public void addNewMsg(String newMsg) {
        messages.add(newMsg);
    }

    @Override
    public void buildMsgDistances() {

    }
}
