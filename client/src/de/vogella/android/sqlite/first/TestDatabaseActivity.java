package de.vogella.android.sqlite.first;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import rejkid.ev.com.ClubItem;
import rejkid.ev.com.IMessageReceiverListener;
import rejkid.ev.com.MessageNotifierData;
import rejkid.ev.com.NioClient;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import static rejkid.ev.com.GlobalConstants.*;

public class TestDatabaseActivity extends ListActivity implements AnimationListener {

  static Logger logger = Logger.getLogger("TestDatabaseActivity.class");

  private static final int CONNECT_TIMEOUT = 1000 * 60 * 5;
  private static final int EXPECTED_CONNECT_TIME = 1000 * 60 * 1;
  private static final int MENU_CONTACT = 0;
  private static final int MENU_BOUNCER = 1;
  private View progressBar;
  private CommentsDataSource datasource;
  protected Handler handler;
  private AlertDialog alertDialog;
  private String name;
  private String iD;
  private AlertDialog bouncerDialog;
//  private NioClient client;
  private Animation slideIn;
  private Animation slideOut;
  private String entryTitle;
  private View titleBar;
  private TextView title;
  private LookupTask lookupTask;

  @Override
  protected void onStop() {
    super.onStop();
    lookupTask.cancel(false);
  }

  public TestDatabaseActivity() throws UnknownHostException, IOException {
    super();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
//    try {
      super.onCreate(savedInstanceState);
      ConfigureLog4J.configure();


    // AlertDialog.Builder builder = new AlertDialog.Builder(this);
    // Context mContext = getApplicationContext();
    // LayoutInflater inflater = (LayoutInflater)
    // mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    // final View layout = inflater.inflate(R.layout.clubdatainput,
    // (ViewGroup) findViewById(R.id.layout_root));
    // builder.setView(layout);
    // builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    //
    // public void onClick(DialogInterface dialog, int id) {
    // TextView text = (TextView) layout.findViewById(R.id.editText1);
    // iD = text.getText().toString();
    // text = (TextView) layout.findViewById(R.id.editText2);
    // name = text.getText().toString();
    // client.ID = iD;
    // client.NAME = name;
    // client.initialClubUpdate();
    // // TestDatabaseActivity.this.finish();
    // }
    // }).setNegativeButton("No", new DialogInterface.OnClickListener() {
    // public void onClick(DialogInterface dialog, int id) {
    // dialog.cancel();
    // }
    // });
    // alertDialog = builder.create();
    //
    // builder = new AlertDialog.Builder(this);
    // mContext = getApplicationContext();
    // inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
    // final View blayout = inflater.inflate(R.layout.bouncer,
    // (ViewGroup) findViewById(R.id.layout_bouncer_root));
    // Button b1 = (Button) blayout.findViewById(R.id.button1);
    // b1.setOnClickListener(new OnClickListener() {
    //
    // @Override
    // public void onClick(View arg0) {
    // // TODO Auto-generated method stub
    // ByteBuffer[] msg = MessageParser.composeAddDeleteMessage(iD, name, MessageParser.ADDB);
    // client.add(msg);
    // }
    // });
    // Button b2 = (Button) blayout.findViewById(R.id.button2);
    // b2.setOnClickListener(new OnClickListener() {
    //
    // @Override
    // public void onClick(View arg0) {
    // // TODO Auto-generated method stub
    // ByteBuffer[] msg = MessageParser.composeAddDeleteMessage(iD, name, MessageParser.ADDG);
    // client.add(msg);
    // }
    // });
    // builder.setView(blayout);
    // bouncerDialog = builder.create();
    //
    // handler = new Handler();
    //
    datasource = new CommentsDataSource(this);
    datasource.open();

    // Load animations used to show/hide progress bar
    slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
    slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);

    // Listen for the "in" animation so we make the progress bar visible
    // only after the sliding has finished.
    slideIn.setAnimationListener(this);

    View header = getLayoutInflater().inflate(R.layout.header, null);

    final ListView lv = getListView();
    lv.setFastScrollEnabled(true);
    lv.addHeaderView(header);
    setListAdapter(new ClubArrayAdapter(this));
    lv.setTextFilterEnabled(true);

    titleBar = findViewById(R.id.title_bar);
    title = (TextView) findViewById(R.id.title);
    progressBar = (ProgressBar) findViewById(R.id.progress);

    IMessageReceiverListener notifier = new IMessageReceiverListener() {
      /*
       * We are ready to receive messages from server after we successfully connect to it
       * 
       * @see rejkid.ev.com.MessageNotifier#messageReceived(rejkid.ev.com.ClubItem)
       */
      @Override
      public synchronized void messageReceived(final MessageNotifierData data) {
        if (data != null && data.getItem() != null) {
          ProcessMessageReceived pms = new ProcessMessageReceived(data.getItem());
          runOnUiThread(pms);
        }
      }
    };
    try {
      lookupTask = new LookupTask(notifier);
      lookupTask.execute(getIntent());
    } catch (Exception e) {
      finish();
      Toast
          .makeText(TestDatabaseActivity.this, "Fatal error: " + e.getMessage(), Toast.LENGTH_LONG)
          .show();
      logger.error("Fatal error occured. The app can't start", e);
    }
  }

  /**
   * Set the title for the current entry.
   */
  protected void setEntryTitle(String entryText) {
    entryTitle = entryText;
    title.setText(entryTitle);
  }

  /**
   * Background task to handle connection to server. This correctly shows and hides the loading
   * animation from the GUI thread before starting a background attempt to connect to the server. When
   * finished, it transitions back to the GUI thread where it updates with the newly established connection.
   */
  private class LookupTask extends AsyncTask<Intent, String, Boolean> {

    /**
     * @param iMessageReceiverListener
     */
    public LookupTask(IMessageReceiverListener iMessageReceiverListener) {
      super();
      this.clientMessageReceiverListener = iMessageReceiverListener;
    }
    private static final int ONE_SECOND_IN_MS = 1000;
    private static final String CONNECTION_EXCEPTION_STR = "Exception connection...";
    private IMessageReceiverListener localMessageReceiverListener;
    private ArrayBlockingQueue<StatusProgressDataHolder> queue = new ArrayBlockingQueue<StatusProgressDataHolder>(1);
    private NioClient client;
    private IMessageReceiverListener clientMessageReceiverListener;

    /**
     * Before jumping into background thread, start sliding in the {@link ProgressBar}. We'll only
     * show it once the animation finishes.
     */
    @Override
    protected void onPreExecute() {
      try {
      client = new NioClient(new InetAddress[] { InetAddress.getByName(REMOTE_SERVER_IP) }, SERVER_PORT);
      client.addMessageListener(clientMessageReceiverListener);
      localMessageReceiverListener = new IMessageReceiverListener() {
        /*
         * We are ready to receive messages from server after we successfully connect to it
         * 
         * @see rejkid.ev.com.MessageNotifier#messageReceived(rejkid.ev.com.ClubItem)
         */
        @Override
        public synchronized void messageReceived(final MessageNotifierData data) {
          try {
            if (data.getStatus() == MessageNotifierData.STATUS.ERROR) {
              String[] status = new String[] { getResources().getString(R.string.ERROR),
                  data.getErrorStr(), getResources().getString(R.string.CONNECTING) };
              queue.put(new StatusProgressDataHolder(Boolean.FALSE, status));
            } else {
              logger.info("Received message:"+data.toString());
              queue.put(new StatusProgressDataHolder(Boolean.TRUE, new String[] {getResources().getString(R.string.OK), getResources().getString(R.string.CONNECTED), getResources().getString(R.string.CONNECTED)}));
            }
            Thread.sleep(5*1000);
          } catch (InterruptedException e) {
            logger.error("Putting polling result error.", e);
          }
        }
      };
      client.addMessageListener(localMessageReceiverListener);
      
      Thread t = new Thread(client);
      t.start();
    } catch (Exception ex) {
      logger.error("Fatal error occured. The app can't start", ex);
      return;
    }

      setEntryTitle(getResources().getString(R.string.CONNECTING));
      titleBar.startAnimation(slideIn);
    }

    /**
     * Perform the background query using {@link ExtendedWikiHelper}, which may return an error
     * message as the result.
     */
    @Override
    protected Boolean doInBackground(Intent... args) {
      while (!isCancelled()) {
        Date beforeConnectTime = new Date();
        StatusProgressDataHolder result;
        // We got the result.
        try {
          client.connectClient();
        } catch (Exception e) {
          String[] status = new String[] { getResources().getString(R.string.ERROR),
              e.getMessage(), CONNECTION_EXCEPTION_STR + e.getMessage() };
          publishProgress(status);
          logger.warn("Could not connect to server", e);
          continue;
        }
        Date afterConnectTime = new Date();
        long connectionDuration = afterConnectTime.getTime() - beforeConnectTime.getTime();
        long time2WaitBeforePollingQueueMs = EXPECTED_CONNECT_TIME - connectionDuration;
        if(time2WaitBeforePollingQueueMs < 0) {
          time2WaitBeforePollingQueueMs = 0;
        }
        long noOfIterations = time2WaitBeforePollingQueueMs/ONE_SECOND_IN_MS;
        boolean time2Quit = false;
        try {
          for(int i = 0; i < noOfIterations; i++) {
            Thread.sleep(ONE_SECOND_IN_MS); // sleep for 1 sec
            if(isCancelled()) {
              time2Quit = true;
              break;
            }
          }
          if(time2Quit) {
            break;
          }
          result = queue.poll(CONNECT_TIMEOUT, TimeUnit.MINUTES);
          // If the CONNECT_TIMEOUT time elapsed (result == null), do the check if we have been cancelled.
          // If not update the progress...
          if (result != null) {
            if(result.status) {
              System.out.println();
              break;
            }
            publishProgress(result.statusStr);
          }
          
          // Wait here...
        } catch (InterruptedException e1) {
          logger.error("Getting polling connection result error.", e1);
        }
      }
      client.removeMessageListener(localMessageReceiverListener);
      client.abort();
      return true;
    }

    /**
     * Our progress update pushes a title bar update.
     */
    @Override
    protected void onProgressUpdate(String... args) {
      String status = args[0];
      if (status.equals(getResources().getString(R.string.ERROR))) {
        logger.info("Called Toast.makeText");
        Toast.makeText(
            TestDatabaseActivity.this,
            getResources().getString(R.string.ADVISORY_PREFIX_STR) + args[1]
                + getResources().getString(R.string.ADVISORY_POSTFIX_STR), Toast.LENGTH_LONG)
            .show();
      }

      if (args.length >= 3) {
        setEntryTitle(args[2]);
      }
    }

    /**
     * When finished, push the newly-found entry content into our {@link WebView} and hide the
     * {@link ProgressBar}.
     */
    @Override
    protected void onPostExecute(Boolean result) {
      client.removeMessageListener(localMessageReceiverListener);

      titleBar.startAnimation(slideOut);
      progressBar.setVisibility(View.INVISIBLE);
      title.setText(getResources().getString(R.string.CONNECTED));      
      logger.info("LookupTask - onPostExecute called");
    }
    class StatusProgressDataHolder {
      String[] statusStr;
      Boolean status;
      /**
       * @param statusStr
       * @param status
       */
      public StatusProgressDataHolder(Boolean status, String... statusStr) {
        super();
        this.statusStr = statusStr;
        this.status = status;
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    logger.error("Destroyed.");
  }

  @Override
  protected void onResume() {
    super.onResume();
    datasource.open();
    logger.error("Resumed.");
  }

  @Override
  protected void onPause() {
    super.onPause();
    datasource.close();
    logger.error("Paused.");
  }

  public class ClubArrayAdapter extends ArrayAdapter<ClubItem> {
    private final Context context;

    public ClubArrayAdapter(Context context) {
      super(context, R.layout.user, R.id.textView1);
      this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      LayoutInflater inflater = (LayoutInflater) context
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View rowView = inflater.inflate(R.layout.user, parent, false);
      TextView textView = (TextView) rowView.findViewById(R.id.textView1);
      ClubItem ci = getItem(position);
      if (ci != null) {
        ProgressBar progress = (ProgressBar) rowView.findViewById(R.id.boys2GirlsRatio);
        textView.setText(getItem(position).getName() + " " + ci.getBoys() + "/" + ci.getGirls());
        int girls = ci.getGirls();
        int boys = ci.getBoys();
        if (girls == 0) {
          if (boys == 0) {
            // do nothing - probably change the background to gray
          } else {
            progress.setMax(boys);
          }
        } else {
          if (boys == 0) {
            progress.setMax(girls);
          } else {
            progress.setMax(girls + boys);
          }
        }
        progress.setProgress(boys);
      } else {
        logger.warn("Got the position that does not exists yet=" + position);
      }

      return rowView;
    }
  }

  @Override
  public void onAnimationEnd(Animation animation) {
    progressBar.setVisibility(View.VISIBLE);
  }

  @Override
  public void onAnimationRepeat(Animation animation) {
    System.out.println("");
  }

  @Override
  public void onAnimationStart(Animation animation) {
    System.out.println("");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // return super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_CONTACT, 0, "Enter your ID");
    menu.add(0, MENU_BOUNCER, 0, "Bouncer");
    return true;

  }

  /* Handles item selections */
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case MENU_CONTACT:
      alertDialog.show();

      return true;
    case MENU_BOUNCER:
      bouncerDialog.show();

      return true;
    }
    ;
    return false;
  }

  class ProcessMessageReceived implements Runnable {
    ClubItem message;

    public ProcessMessageReceived(final ClubItem message) {
      this.message = message;
    }

    @Override
    public void run() {
      final ListView lv = getListView();
      ClubItem item = null;
      ClubArrayAdapter list = ((ClubArrayAdapter) getListAdapter());
      for (int i = 0; i < list.getCount(); i++) {
        ClubItem ci = (ClubItem) list.getItem(i);
        String src = ci.getId();
        String dest = message.getId();
        System.out.println("");
        if (ci.getId().equals(message.getId())) {
          item = ci;
          ci.setBoys(message.getBoys());
          ci.setGirls(message.getGirls());
          lv.invalidateViews();
          break;
        }
      }
      if (item == null) {
        list.add(message);
      }
    }
  }
}
