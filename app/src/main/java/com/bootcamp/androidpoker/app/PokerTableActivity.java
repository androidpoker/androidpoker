package com.bootcamp.androidpoker.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity that runs on the tablet and shows the poker table.
 */
public class PokerTableActivity extends PokerActivity {

    private static final boolean PLAYING_WITH_BOTS = true;
    private static final TableType TABLE_TYPE = TableType.NO_LIMIT;
    /** The size of the big blind. */
    private static final int BIG_BLIND = 10;
    /** The starting cash per player. */
    private static final int STARTING_CASH = 500;

    class User {

  }

  // maybe User object instead of string
  List<User> usersConnected = new ArrayList<User>();

    PlayerListAdapter playersAdapter;
    Handler mainHandler;

  public void setUsersConnected(List<User> users) {
    usersConnected.clear();
    usersConnected.addAll(users);
  }

  public void addUser(User user) {
    usersConnected.add(user);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_poker_table);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      mainHandler = new Handler(getMainLooper());

      Intent discoverableIntent = new
              Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
      discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
      startActivityForResult(discoverableIntent, 0);
  }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        runGame();
    }

    public void displayPlayersInfo(final Map<String, Player> players) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    displayPlayersInfo(players);
                }
            });
            return;
        }
        FragmentManager manager = getFragmentManager();
        PlayerListFragment fragment = (PlayerListFragment) manager.findFragmentById(R.id.players);
        playersAdapter = new PlayerListAdapter(PokerTableActivity.this, players);
        fragment.getListView().setAdapter(playersAdapter);
  }


  private void runGame() {
      new AsyncTask<Void, Void, Void>() {

          private BluetoothServerSocket mmServerSocket;

          @Override
          protected void onPreExecute() {
              try {
                  // MY_UUID is the app's UUID string, also used by the client code
                  mmServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(
                          "androidpoker", PokerService.uuid);
              } catch (IOException e) {
                  Toast.makeText(PokerTableActivity.this, "socket opening failed", Toast.LENGTH_SHORT).show();
              }
          }

          protected Void doInBackground(Void... args) {
              BluetoothSocket socket = null;
              // Keep listening until exception occurs or a socket is returned
              Map<String, Player> players = new HashMap<String, Player>();

              while (players.size() < 4) {
                  try {
                      socket = mmServerSocket.accept();
                  } catch (IOException e) {
                      break;
                  }
                  // If a connection was accepted
                  if (socket != null) {
                      // Do work to manage the connection (in a separate thread)
                      players.put("Henry " + players.size(), new Player("Henry " + players.size(),  STARTING_CASH, new NetworkPlayer(socket)));

                  }
              }

              // Comment out
//              players.put("Eddie",  new Player("Eddie", STARTING_CASH, new BasicBot(50, 25)));

              UITableObserver tableObserver = new UITableObserver(PokerTableActivity.this);
              displayPlayersInfo(players);

              Table table = new Table(TABLE_TYPE, tableObserver, BIG_BLIND);
              for (Player player : players.values()) {
                  table.addPlayer(player);
              }
              try {
                  Thread.sleep(5000);
              } catch (Exception e) {

              }
              table.run();
              return null;
          }

          protected void onProgressUpdate(Void... progress) {
              // Do nothing.
          }

          protected void onPostExecute(Void result) {
              // Do nothing.
          }
      }.execute();
    }

    /* register the broadcast receiver with the intent values to be matched */
  @Override
  protected void onResume() {
    super.onResume();
//    doBindService(new BindingCallback() {
//        @Override
//        public void onBoundToService() {
//            mBoundService.registerMessageListener(PokerTableActivity.this);
//            mBoundService.startReceivingMessages();
//            Toast.makeText(PokerTableActivity.this, "started receiving messages!", Toast.LENGTH_SHORT).show();
//        }
//    });
  }
  /* unregister the broadcast receiver */
  @Override
  protected void onPause() {
    super.onPause();
    doUnbindService();
  }

  public static class CardsFragment extends Fragment {
    @Override
    public View onCreateView(
        LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState) {
      // Inflate the layout for this fragment
      return inflater.inflate(R.layout.fragment_cards_on_table, container, false);
    }
  }

  public static class PlayerListFragment extends Fragment {
    ListView playerList;
    @Override
    public View onCreateView(
        LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState) {
      // Inflate the layout for this fragment
      playerList = (ListView) inflater.inflate(R.layout.fragment_player_list, container, false);
      return playerList;
    }

    public ListView getListView() {
        return playerList;
    }
  }

    public class UITableObserver implements TableObserver {
        private final String TAG = UITableObserver.class.getSimpleName();

        public UITableObserver(Context context) {

        }

        public void messageReceived(final String message) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
//                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
                    Log.d(TAG, "messageReceived");
                }
            });

        }

        public void joinedTable(TableType type, int bigBlind, List<Player> players) {
            Log.d(TAG, "joinedTable");
        }

        public void handStarted(Player dealer) {
            Log.d(TAG, "handStarted");

        }

        public void actorRotated(final Player actor) {
            Log.d(TAG, "actorRotated");

            slowDownBots();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    playersAdapter.rotatePlayer(actor);
                }
            });
        }

        public void playerUpdated(final Player player) {
            slowDownBots();

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    playersAdapter.updatePlayer(player);
                }
            });
        }

        public void boardUpdated(final List<Card> cards, final int bet, final int pot) {
            slowDownBots();

            final List<Card> immutableCards = new ArrayList<Card>(cards);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    ((TextView)findViewById(R.id.bet)).setText("Bet" + " $" + bet);
                    ((TextView)findViewById(R.id.pot)).setText("Pot" + " $" + pot);
                    int noOfCards = (immutableCards == null) ? 0 : immutableCards.size();
                    for (int i = 0; i < noOfCards; i++) {
                        ImageView imageView = null;
                        if (i == 0)
                            imageView = (ImageView) findViewById(R.id.first_card);
                        else if (i == 1)
                            imageView = (ImageView) findViewById(R.id.second_card);
                        else if (i == 2)
                            imageView = (ImageView) findViewById(R.id.third_card);
                        else if (i == 3)
                            imageView = (ImageView) findViewById(R.id.fourth_card);
                        else if (i == 4)
                            imageView = (ImageView) findViewById(R.id.fifth_card);
                        Card currentCard = immutableCards.get(i);
                        String rank = Card.RANK_SYMBOLS[currentCard.getRank()];
                        char suit = Card.SUIT_SYMBOLS[currentCard.getSuit()];
                        String cardName = "card_" + rank + suit;
                        int id = getResources().getIdentifier(cardName, "drawable", getPackageName());
                        imageView.setImageResource(id);
                    }
                    for (int i = noOfCards; i < 5; i++) {
                        ImageView imageView = null;
                        if (i == 0)
                            imageView = (ImageView) findViewById(R.id.first_card);
                        else if (i == 1)
                            imageView = (ImageView) findViewById(R.id.second_card);
                        else if (i == 2)
                            imageView = (ImageView) findViewById(R.id.third_card);
                        else if (i == 3)
                            imageView = (ImageView) findViewById(R.id.fourth_card);
                        else if (i == 4)
                            imageView = (ImageView) findViewById(R.id.fifth_card);
                        int id = getResources().getIdentifier("card_blue_back", "drawable", getPackageName());
                        imageView.setImageResource(id);
                    }
                }
            });
        }

        public void playerActed(Player player) {
            Log.d(TAG, "playerActed");
        }
    }

    public void slowDownBots() {
        if (!PLAYING_WITH_BOTS) {
            return;
        }
        try {
            Thread.sleep(50);
        } catch (Exception e) {

        }
    }
}
