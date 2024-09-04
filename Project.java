import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.LinkedList;
import java.util.TreeMap;

public class Spotify {
  private ArrayList<Song> songs;
  private HashMap<String, Song> songsByTitle;
  private SongNode currentSong;
  private Map<Song, Integer> songFrequency;
  private PriorityQueue<Song> topSongs;
  private LFUCache lfuCache;

  private class SongNode {
    private Song song;
    private SongNode prev;
    private SongNode next;

    public SongNode(Song song) {
      this.song = song;
      prev = null;
      next = null;
    }

    public Song getSong() {
      return song;
    }
  }

  public Spotify() {
    songs = new ArrayList<>();
    songsByTitle = new HashMap<>();
    songFrequency = new HashMap<>();
    topSongs = new PriorityQueue<>((a, b) -> songFrequency.get(b) - songFrequency.get(a));
    currentSong = null;
    lfuCache = new LFUCache(100);
  }

  public void addSong(String title, String artist, int duration) {
    Song newSong = new Song(title, artist, duration);
    songs.add(newSong);
    songsByTitle.put(title, newSong);
    songFrequency.put(newSong, 0);
    lfuCache.put(newSong, 0);
    if (currentSong == null) {
      currentSong = new SongNode(newSong);
    }
  }

  public Song getSongByTitle(String title) {
    return songsByTitle.get(title);
  }

  public void playCurrentSong() {
    if (currentSong != null) {
      System.out.println("Now playing: " + currentSong.getSong());
      incrementSongFrequency(currentSong.getSong());
    }
  }

  public void playNextSong() {
    if (currentSong != null && currentSong.next != null) {
      currentSong = currentSong.next;
      System.out.println("Now playing: " + currentSong.getSong());
      incrementSongFrequency(currentSong.getSong());
    }
  }

  public void playPreviousSong() {
    if (currentSong != null && currentSong.prev != null) {
      currentSong = currentSong.prev;
      System.out.println("Now playing: " + currentSong.getSong());
      incrementSongFrequency(currentSong.getSong());
    }
  }

  private void incrementSongFrequency(Song song) {
    if (songFrequency.containsKey(song)) {
      int frequency = songFrequency.get(song);
      songFrequency.put(song, frequency + 1);

      if (topSongs.contains(song)) {
        topSongs.remove(song);
      }
      topSongs.offer(song);

      if (topSongs.size() > 10) {
        topSongs.poll();
      }

      lfuCache.put(song, songFrequency.get(song));
    }
  }

  public void printTopSongs() {
    System.out.println("Top 10 songs:");
    for (Song song : topSongs) {
      System.out.println(song.getTitle() + " by " + song.getArtist() + " - " + songFrequency.get(song) + " plays");
    }

    System.out.println("Top 10 favourite songs:");
    LinkedList<LFUCache.ListNode> topLFU = lfuCache.getTopN(10);
    for (LFUCache.ListNode node : topLFU) {
      Song song = (Song) node.key;
      int frequency = node.value;
      System.out.println(song.getTitle() + " by " + song.getArtist() + " - " + frequency + " plays");
    }
  }

  public static void main(String[] args) {
    Spotify mySpotify = new Spotify();
    mySpotify.addSong("Shape of You", "Ed Sheeran", 235);
    mySpotify.addSong("Uptown Funk", "Mark Ronson ft. Bruno Mars", 270);
    mySpotify.addSong("Bohemian Rhapsody", "Queen", 354);

    mySpotify.playCurrentSong();

    mySpotify.playNextSong();
    mySpotify.playPreviousSong();

    Song mySong = mySpotify.getSongByTitle("Uptown Funk");
    System.out.println("Song found: " + mySong);

    mySpotify.playNextSong();
    mySpotify.playNextSong();
    mySpotify.playNextSong();
    mySpotify.playNextSong();
    mySpotify.playNextSong();
    mySpotify.playNextSong();
    mySpotify.playNextSong();
    mySpotify.playNextSong();
    mySpotify.playNextSong();
    mySpotify.playNextSong();

    mySpotify.printTopSongs();
  }
}

class LFUCache {
  private int capacity;
  private int size;
  private Map<Object, ListNode> keyToNodeMap;
  private TreeMap<Integer, LinkedList<ListNode>> freqToNodesMap;

  public LFUCache(int capacity) {
    this.capacity = capacity;
    this.size = 0;
    this.keyToNodeMap = new HashMap<>();
    this.freqToNodesMap = new TreeMap<>();
  }

  public int get(Object key) {
    if (!keyToNodeMap.containsKey(key)) {
      return -1;
    }

    ListNode node = keyToNodeMap.get(key);
    updateFrequency(node);
    return node.value;
  }

  public void put(Object key, int value) {
    if (capacity <= 0) {
      return;
    }

    if (keyToNodeMap.containsKey(key)) {
      ListNode node = keyToNodeMap.get(key);
      node.value = value;
      updateFrequency(node);
    } else {
      if (size >= capacity) {
        removeLFU();
      }

      ListNode newNode = new ListNode(key, value, 1);
      keyToNodeMap.put(key, newNode);
      freqToNodesMap.computeIfAbsent(1, k -> new LinkedList<>()).addFirst(newNode);
      size++;
    }
  }

  private void updateFrequency(ListNode node) {
    LinkedList<ListNode> currentList = freqToNodesMap.get(node.freq);
    currentList.remove(node);

    if (currentList.isEmpty()) {
      freqToNodesMap.remove(node.freq);
    }

    node.freq++;
    freqToNodesMap.computeIfAbsent(node.freq, k -> new LinkedList<>()).addFirst(node);
  }

  private void removeLFU() {
    LinkedList<ListNode> leastFrequentList = freqToNodesMap.firstEntry().getValue();
    ListNode nodeToRemove = leastFrequentList.removeLast();

    if (leastFrequentList.isEmpty()) {
      freqToNodesMap.remove(freqToNodesMap.firstKey());
    }

    keyToNodeMap.remove(nodeToRemove.key);
    size--;
  }

  public LinkedList<ListNode> getTopN(int n) {
    LinkedList<ListNode> topN = new LinkedList<>();

    for (LinkedList<ListNode> list : freqToNodesMap.descendingMap().values()) {
      for (ListNode node : list) {
        topN.add(node);

        if (topN.size() == n) {
          return topN;
        }
      }
    }

    return topN;
  }

  static class ListNode {
    Object key;
    int value;
    int freq;

    ListNode(Object key, int value, int freq) {
      this.key = key;
      this.value = value;
      this.freq = freq;
    }
  }
}
