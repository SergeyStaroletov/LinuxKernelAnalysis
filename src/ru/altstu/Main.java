package ru.altstu;

import javafx.util.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class Main {


    /**
     * Sorts a map by the value (desc)
     *
     * @param map
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Computes destance between 2 strings
     *
     * @param s
     * @param t
     * @return
     */
    public static int ComputeLevenshteinDistance(String s, String t) {
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


    public static void main(String[] args) throws IOException, GitAPIException {
        //Repository repo = new FileRepository("/Users/sergey/IdeaProjects/bluez/.git");
        // Repository repo = new FileRepository("/Users/sergey/IdeaProjects/CalculatorTDD/.git");
        Repository repo = new FileRepository("/Users/sergey/Projects/to_analize/linux/.git");

        String pathInGit = "/";

        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(repo);
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);

        // ByteArrayOutputStream os = new ByteArrayOutputStream();
        DiffFormatter df2 = new DiffFormatter(System.out);
        RawTextComparator cmp = RawTextComparator.DEFAULT;

        df2.setRepository(repo);
        df2.setDiffComparator(cmp);
        df2.setDetectRenames(true);


        //TreeWalk treeWalk = new TreeWalk(repo);

        Git git = new Git(repo);
        RevWalk walk = new RevWalk(repo);

        List<String> messages = new LinkedList<String>();
        Map<String, Integer> msgRelevance = new HashMap<String, Integer>();

        //map (filename, pos) -> count changes
        Map<KeyFilePos, Integer> mapFileChanges = new HashMap<KeyFilePos, Integer>();

        //map filename -> count changes
        Map<String, Integer> mapFileNameChanges = new HashMap<String, Integer>();


        List<Ref> branches = git.branchList().call();

        for (Ref branch : branches) {
            String branchName = branch.getName();

            System.out.println("Commits of branch: " + branch.getName());
            System.out.println("-------------------------------------");


            Iterable<RevCommit> commits;

            if (pathInGit.equals("") || pathInGit.equals("/")) commits = git.log().all().call();
            else {
                LogCommand logCommand = git.log()
                        .add(git.getRepository().resolve(Constants.HEAD))
                        .addPath(pathInGit);
                commits = logCommand.call();
            }


            int count = 0;
            for (RevCommit commit : commits) {
                count++;
            }

            if (pathInGit.equals("") || pathInGit.equals("/")) commits = git.log().all().call();
            else {
                LogCommand logCommand = git.log()
                        .add(git.getRepository().resolve(Constants.HEAD))
                        .addPath(pathInGit);
                commits = logCommand.call();
            }

            int current = 0;


            for (RevCommit commit : commits) {

                current++;
                TreeWalk treeWalk = new TreeWalk(repo);
                RevCommit parent;
                try {
                    parent = commit.getParent(0);
                } catch (Exception ex) {
                    break;
                }
                //treeWalk.reset(commit.getTree());


                boolean foundInThisBranch = false;

                RevCommit targetCommit = walk.parseCommit(repo.resolve(
                        commit.getName()));
                for (Map.Entry<String, Ref> e : repo.getAllRefs().entrySet()) {
                    if (e.getKey().startsWith(Constants.R_HEADS)) {
                        if (walk.isMergedInto(targetCommit, walk.parseCommit(
                                e.getValue().getObjectId()))) {
                            String foundInBranch = e.getValue().getName();
                            if (branchName.equals(foundInBranch)) {
                                foundInThisBranch = true;
                                break;
                            }
                        }
                    }
                }

                if (foundInThisBranch) {

                    //System.out.println(commit.getFullMessage());

                    String newMsg = commit.getFullMessage();

                    int pos = newMsg.indexOf("Fix");
                    Boolean isFixes = false;

                    int posFixes = newMsg.indexOf("Fixes:");
                    if (posFixes == pos) isFixes = true;

                    //int pos = 0;
                    if (pos != -1) {

                        if (!isFixes)
                            pos += 3;
                        else
                            pos += 6;//for messages with Fixes:

                        int end = newMsg.indexOf('\n', pos);

                        if (end != -1) newMsg = newMsg.substring(pos, end);
                        else newMsg = newMsg.substring(pos);
                        System.out.println("Commit : " + String.valueOf(current) + "/" + String.valueOf(count) + ", branch = " + branch.getName());
                        PersonIdent authorIdent = commit.getAuthorIdent();
                        Date authorDate = authorIdent.getWhen();
                        System.out.println(authorIdent.getName() + " commited on " + authorDate);
                        System.out.println(newMsg);

                        messages.add(newMsg);

                        //find the nearest string

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

                        Integer countR = msgRelevance.get(newMsg);
                        if (countR == null) {
                            msgRelevance.put(newMsg, 1);
                        } else {
                            msgRelevance.put(newMsg, countR + 1);
                        }

                        if (strMin.length() > 0) {
                            countR = msgRelevance.get(strMin);
                            if (countR == null) {
                                msgRelevance.put(strMin, 1);
                            } else {
                                msgRelevance.put(strMin, countR + 1);
                            }
                        }

                        System.out.println("The closest msg is : " + strMin);

                        //detect changes in files
                        List<DiffEntry> diffs;

                        diffs = df.scan(parent.getTree(), commit.getTree());
                        for (DiffEntry diff : diffs) {


                            FileHeader header = df.toFileHeader(diff);
                            // System.out.println(header.toString());
                            EditList list = header.toEditList();

                            String name = header.getNewPath();//имя файла с изменениями

                           // if (!pathInGit.equals("") && name.startsWith(pathInGit))
                                for (Edit edit : list) {
                                    // System.out.println(edit);
                                    //linesDeleted += edit.getEndA() - edit.getBeginA();
                                    //linesAdded += edit.getEndB() - edit.getBeginB();

                                    //обновить общий map по имени файла
                                    Integer countTot = mapFileNameChanges.get(name);
                                    if (countTot == null) {
                                        mapFileNameChanges.put(name, 1);
                                    } else {
                                        mapFileNameChanges.put(name, countTot + 1);
                                    }


                                    for (int line = edit.getEndB(); line <= edit.getBeginB(); line++) {
                                        //обновить map по файлу и по линии
                                        KeyFilePos key = new KeyFilePos(name, line);
                                        Integer countLS = mapFileChanges.get(key);
                                        if (countLS == null) {
                                            mapFileChanges.put(key, 1);
                                        } else {
                                            mapFileChanges.put(key, countLS + 1);
                                        }


                                    }


                                }
                        }

                        System.out.println("*********************************************");

                    }

                }
            }
        }

        //sort the map of fixes
        msgRelevance = sortByValue(msgRelevance);


        System.out.println("**************************************");
        System.out.println("The most 35 frequent errors:");

        int c = 0;
        for (Map.Entry<String, Integer> entry : msgRelevance.entrySet()) {
            if (c++ > 35) break;
            System.out.println(entry.getKey() + "/" + entry.getValue());
        }

        System.out.println("**************************************");
        System.out.println("The most frequent files with changes:");

        //sort the map of filechanges
        mapFileNameChanges = sortByValue(mapFileNameChanges);
        c = 0;
        for (Map.Entry<String, Integer> entry : mapFileNameChanges.entrySet()) {
            if (c++ > 20) break;
            String fileName = entry.getKey();

            System.out.println("Filename: " + fileName + "/" + entry.getValue());

            /*
            LogCommand logCommand = git.log()
                .add(git.getRepository().resolve(Constants.HEAD))
                .addPath(fileName);


            Iterable<RevCommit> iter = logCommand.call();
            */

            //track the most changed lines
            List<Pair<Integer, Integer>> pairsList = new ArrayList<Pair<Integer, Integer>>();

            for (Map.Entry<KeyFilePos, Integer> mapp : mapFileChanges.entrySet()) {
                //count all (position, count) for that filename
                if (mapp.getKey().fileName.equals(entry.getKey())) {
                    pairsList.add(new Pair<Integer, Integer>(mapp.getKey().position, mapp.getValue()));
                }
            }

            //sort list pairs
            pairsList.sort(Comparator.comparing(p -> -p.getValue()));
            //show top 10 pairs
            int ccc = 0;
            for (Pair<Integer, Integer> pair : pairsList) {
                if (ccc++ > 10) break;

                Integer lineToCheck = pair.getKey();
                System.out.println(" Line:" + lineToCheck + ", changes -> " + pair.getValue());

                    /*
                    // look for commits for the Line
                    for (RevCommit revCommit : iter) {
                        String newMsg = revCommit.getFullMessage();
                        int pos = newMsg.indexOf("Fix"); //if it is fix
                        //int pos = 0;
                        if (pos != -1) {
                            //and if it has
                            RevCommit parentC;
                            try {
                                parentC = revCommit.getParent(0);
                            } catch (Exception ex) {
                                continue;
                            }
                            //get the diff

                            List<DiffEntry> diffs;
                            diffs = df.scan(parentC.getTree(), revCommit.getTree());
                            for (DiffEntry diff : diffs) {
                                FileHeader header = df.toFileHeader(diff);
                                EditList list = header.toEditList();

                                if (header.getNewPath().equals(fileName)) {
                                    for (Edit edit : list) {
                                        //System.out.println(edit);
                                        if (lineToCheck >= edit.getBeginB() && lineToCheck <= edit.getEndB()) {
                                            //our line, print the change
                                            System.out.println("   change: " + df2.toString());
                                        }
                                        //linesDeleted += edit.getEndA() - edit.getBeginA();
                                        //linesAdded += edit.getEndB() - edit.getBeginB();

                                    }
                                }
                            }
                        }
                    }*/


            }
        }


    }


}