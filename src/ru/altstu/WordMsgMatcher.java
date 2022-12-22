package ru.altstu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;


public class WordMsgMatcher implements IMsgMatcher {

  HashMap<String, String> engWords = new HashMap<>();
  List<String> uniqueWords;
  java.util.concurrent.atomic.AtomicIntegerArray  weights;
  List<String> messages = new ArrayList<>();
  List<HashMap<Integer, Double>> features = new ArrayList<>();
  List<Double> sizes = new ArrayList<>();
  private boolean tfidf = true;

  public WordMsgMatcher() throws Exception {
    loadWords();
  }

  private void loadWords() throws Exception {
    File file = new File("English-Morph.txt");
    FileReader fr = new FileReader(file);
    BufferedReader br = new BufferedReader(fr);
    String line;
    while ((line = br.readLine()) != null) {
      String cols[] = line.split("\t");
      if (cols.length > 1) {
        engWords.put(cols[0], cols[1]);
      }
    }
    fr.close();
    // prepare the list of all words
    uniqueWords = new ArrayList<>(engWords.size());
  }


  @Override
  public String closestMessage(String newMsg) {
    return null;
  }

  @Override
  public void addNewMsg(String newMsg) {
    newMsg = newMsg.toLowerCase().trim();
    String words[] = newMsg.split("[ !\"\\#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]+");
    HashMap<Integer, Double> feature = new HashMap<>();
    double sz = 0;
    for (String word : words) if (word.trim() != "") {
      String rightWord = word;
      if (engWords.containsKey(word)) {
        // we have this word in the English dict
        rightWord = engWords.get(word);
      }
      // add a word to the all words collection
      if (!uniqueWords.contains(rightWord)) {
        uniqueWords.add(rightWord);
      }
      int pos = uniqueWords.indexOf(rightWord); // number of word in the list of all words
      feature.put(pos, 1.0);
      sz += 1;
    }
    if (sz > 0 && !messages.contains(newMsg)) {
      messages.add(newMsg);
      features.add(feature);
      sizes.add(Math.sqrt(sz));
    }
  }

  public void buildMsgDistances() throws InterruptedException {
    if (tfidf) {
      int d = uniqueWords.size();
      // calculate idfs
      double[] idfs = new double[d];
      for (int w = 0; w < d; w++) {
        int idf = 0;
        for (HashMap<Integer, Double> feature : features) {
          if (feature.containsKey(w)) idf++;
        }
        idfs[w] = Math.log(1.0 * d / idf);
      }
      // calculate tf and fix the vectors
      int f = 0;
      for (HashMap<Integer, Double> feature : features) {
        double newSz = 0;
        double tf = 1.0 / feature.size();
        for (Integer k : feature.keySet()) {
          double newEl = feature.get(k) * (tf * idfs[k]);
          feature.put(k, newEl);
          newSz += newEl*newEl;
        }
        sizes.set(f++, Math.sqrt(newSz));
      }
    }

    System.out.println("calculating distances...");
    weights = new java.util.concurrent.atomic.AtomicIntegerArray(messages.size()); //for each message we will store its weight
    final Object ws = new Object();

    // iteration for all messages should be parallelized
    int cores = Runtime.getRuntime().availableProcessors();
    int msgLen = messages.size();
    Vector<Thread> threads = new Vector<>(cores);
    for (int p = 0; p < cores; p++) {
      final int startI = p * msgLen / cores;
      final int endI = (p != (cores - 1)) ? startI + (msgLen / cores): msgLen;
      final int myP = p;
      Thread t = new Thread(
          (Runnable) () -> {
            for (int i = startI; i < endI; i++)
              for (int j = 0; j < messages.size(); j++)
                if (i != j) {
                  HashMap<Integer, Double> vector1 = features.get(i);
                  HashMap<Integer, Double> vector2 = features.get(j);
                  double diff = 0;
                  for (int f = 0; f < uniqueWords.size(); f++) {
                    double p1 = 0;
                    if (vector1.containsKey(f)) p1 = vector1.get(f);
                    double p2 = 0;
                    if (vector2.containsKey(f)) p2 = vector2.get(f);
                    diff += p1 * p2;
                  }
                  diff /= (sizes.get(i) * sizes.get(j));
                  diff = 1.0 - diff;
                  //System.out.println("t=" + myP+ ") i = " + i + " diff '" + messages.get(i)+ "' vs '" + messages.get(j) + "' = " + diff);
                  if (diff < 0.7) {
                    weights.getAndIncrement(i); //[i]++;
                    weights.getAndIncrement(j);
                  }
                }
          });
      threads.add(t);
      t.start();
    }
    for (int p = 0; p < cores; p++) {
      threads.get(p).join();
    }

    // sort msgs by the weights
    // do now finding max for top times (compl max*O(n))
    final int top = 20;//todo: set top
    Vector<String> relevantMsgs = new Vector<>(top);
    for (int total = 0; total < top; total++) {
      int max = -1;
      int maxI = 0;
      for (int i = 0; i < weights.length(); i++)
        if (weights.get(i) > max) {
          maxI = i;
          max = weights.get(i);
        }
      relevantMsgs.add(messages.get(maxI) + " / " + weights.get(maxI));
      weights.set(maxI, 0);
    }
    // print
    for (String msg: relevantMsgs) {
      System.out.println(msg);
    }


  }
}
