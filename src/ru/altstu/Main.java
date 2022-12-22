package ru.altstu;

import javafx.util.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class Main {


  public static boolean CheckIfFixMessage(String msg) {
    int pos = msg.toLowerCase().indexOf("fix");
    return (pos != -1);
  }

  public static boolean IfInBranch(Repository repo, RevWalk walk,RevCommit commit, String branchName) throws IOException {
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
    return foundInThisBranch;
  }

  public static Iterable<RevCommit> obtainCommits(Git git, String pathInGit) throws IOException, GitAPIException {
    Iterable<RevCommit> commits;
    if (pathInGit.equals("") || pathInGit.equals("/")) commits = git.log().all().call();
    else {
      LogCommand logCommand = git.log()
          .add(git.getRepository().resolve(Constants.HEAD))
          .addPath(pathInGit);
      commits = logCommand.call();
    }
    return commits;
  }

  //map (filename, pos) -> count changes
  static Map<KeyFilePos, Integer> mapFileChanges = new HashMap<KeyFilePos, Integer>();
  //map filename -> count changes
  static Map<String, Integer> mapFileNameChanges = new HashMap<String, Integer>();

  //detect changes in files
  public static void detectChanges(DiffFormatter df, RevCommit parent, RevCommit commit) throws IOException {
    List<DiffEntry> diffs;
    diffs = df.scan(parent.getTree(), commit.getTree());
    for (DiffEntry diff : diffs) {
      FileHeader header = df.toFileHeader(diff);
      EditList list = header.toEditList();
      String name = header.getNewPath();//имя файла с изменениями
      // if (!pathInGit.equals("") && name.startsWith(pathInGit))
      for (Edit edit : list) {
        // System.out.println(edit);
        //linesDeleted += edit.getEndA() - edit.getBeginA();
        //linesAdded += edit.getEndB() - edit.getBeginB();

        // fix common map by filename
        Integer countTot = mapFileNameChanges.get(name);
        if (countTot == null) {
          mapFileNameChanges.put(name, 1);
        } else {
          mapFileNameChanges.put(name, countTot + 1);
        }

        for (int line = edit.getEndB(); line <= edit.getBeginB(); line++) {
          // fix common map by filename and line
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
  }

  // Sorts a map by the value (desc)
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






  /*
  Analyze repository given by the repo constructor
   */
  public static void main(String[] args) throws Exception {

    long startTime = System.currentTimeMillis();

    IMsgMatcher matcher = new WordMsgMatcher();

    // Repository repo = new FileRepository("/Users/sergey/IdeaProjects/bluez/.git");
    Repository repo = new FileRepository("/Users/sergey/inria/linux/.git");
    String pathInGit = "drivers/thunderbolt/"; //change path here for example, to "mm" in linux kernel git
    //String pathInGit = "mm/"; //change path here for example, to "mm" in linux kernel git

    DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
    df.setRepository(repo);
    df.setDiffComparator(RawTextComparator.DEFAULT);
    df.setDetectRenames(true);
    DiffFormatter df2 = new DiffFormatter(System.out);
    RawTextComparator cmp = RawTextComparator.DEFAULT;
    df2.setRepository(repo);
    df2.setDiffComparator(cmp);
    df2.setDetectRenames(true);
    Git git = new Git(repo);
    RevWalk walk = new RevWalk(repo);

    List<Ref> branches = git.branchList().call();

    for (Ref branch : branches) {
      String branchName = branch.getName();
      System.out.println("Found branch: " + branch.getName());
    }

    for (Ref branch : branches) {
      String branchName = branch.getName();
      System.out.println("Commits of branch: " + branch.getName());
      System.out.println("-------------------------------------");
      Iterable<RevCommit> commits = obtainCommits(git, pathInGit);
      int count = 0;
      for (RevCommit commit : commits) {
        count++;
      }
      // again, because of the iterator
      commits = obtainCommits(git, pathInGit);

      // record all commit messages
      int current = 0;
      for (RevCommit commit : commits) {
        RevCommit parent;
        try {
          parent = commit.getParent(0);
        } catch (Exception ex) {
          continue;
        }

        if (IfInBranch(repo, walk, commit, branchName)) {
          String newMsg = commit.getFullMessage();
          String lines[] = newMsg.split("\n");
          for (String line: lines) {
            // for multi-line commit message do we fix something?
            if (CheckIfFixMessage(line))
              matcher.addNewMsg(line);
          }
        }
        detectChanges(df, parent, commit);
        if ( (current * 100 /(count + 1)) % 10 == 0)
          System.out.println(current + " / " + count);
        current++;
      }

      long commitProcessTime = System.currentTimeMillis();
      System.out.println("Time to process commits: " + ((commitProcessTime - startTime) / 1000) + " sec" );

      matcher.buildMsgDistances();
      long endTime = System.currentTimeMillis();
      System.out.println("Time of work: " + ((endTime - startTime) / 1000) + " sec" );

    }



    System.out.println("**************************************");
    System.out.println("The most frequent files with changes:");

    //sort the map of filechanges
    mapFileNameChanges = sortByValue(mapFileNameChanges);
    int c = 0;
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