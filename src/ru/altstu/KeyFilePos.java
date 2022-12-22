package ru.altstu;

/**
 * Created by sergey on 31/03/2017.
 */
public class KeyFilePos {
  public String fileName;
  public Integer position;

  public KeyFilePos(String fileName, Integer position) {
    this.fileName = fileName;
    this.position = position;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof KeyFilePos))
      return false;
    KeyFilePos ref = (KeyFilePos) obj;
    return this.fileName.equals(ref.fileName) &&
        this.position.equals(ref.position);
  }

  @Override
  public int hashCode() {
    return fileName.hashCode() ^ position.hashCode() ;
  }
}