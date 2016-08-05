package ca.jeffrey.apodgallery;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bluejamesbond.text.DocumentView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.sothree.slidinguppanel.FloatingActionButtonLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    // Logging tag for listeners
    final SimpleDateFormat EXPANDED_FORMAT = new SimpleDateFormat("MMMM d, y");
    final SimpleDateFormat NUMERICAL_FORMAT = new SimpleDateFormat("y-MM-dd");
    final SimpleDateFormat SHORT_FORMAT = new SimpleDateFormat("yyMMdd");
    final String DEFAULT_IMAGE_DIRECTORY = Environment.getExternalStorageDirectory().getPath() +
            File.separator + "APOD";
    final String IMAGE_EXT = ".jpg";

    // First available APOD date
    final Calendar MIN_DATE = new GregorianCalendar(1995, 9, 22);

    // NASA API key
    // final private String API_KEY = "***REMOVED***";
    AutoResizeTextView titleText;
    DocumentView description;
    FloatingActionButton fab;
    FloatingActionButtonLayout fabLayout;
    ImageView imageView;
    ImageView tomorrow;
    ImageView yesterday;
    LinearLayout mainView;
    ProgressBar progressBar;
    SlidingUpPanelLayout slidingPanel;
    TextView dateText;
    boolean tooEarly;
    String date;
    String today;
    String imgUrl;
    String sdUrl;
    SharedPreferences sharedPref;

    public static Calendar dateToCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        if (myToolbar != null) {
            myToolbar.showOverflowMenu();
        }

        // Initialize preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Initiate image views
        imageView = (ImageView) findViewById(R.id.image);
        yesterday = (ImageView) findViewById(R.id.left_chevron);
        tomorrow = (ImageView) findViewById(R.id.right_chevron);

        // Other views
        dateText = (TextView) findViewById(R.id.date);
        description = (DocumentView) findViewById(R.id.description);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabLayout = (FloatingActionButtonLayout) findViewById(R.id.fab_layout);
        mainView = (LinearLayout) findViewById(R.id.main_view);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        titleText = (AutoResizeTextView) findViewById(R.id.title);
        tooEarly = false;

        // Set scrollable description text
        if (description != null)
            description.setVerticalScrollBarEnabled(true);

        // Set date view
        today = date = EXPANDED_FORMAT.format(new Date());
        dateText.setText(date);

        // Set image
        getImageData(date);

        // No "tomorrow" image available if default day is "today"
        tomorrow.setVisibility(View.INVISIBLE);
        tomorrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                nextDay();
            }
        });

        yesterday.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                previousDay();
            }
        });

        dateText.setOnClickListener(new View.OnClickListener() {
            Calendar calendar = Calendar.getInstance();

            @Override
            public void onClick(View v) {
                /* Date Picker Library #2 (Does not allow for disabled dates, only enabled dates) */
                Calendar today = Calendar.getInstance();
                Calendar currentDate = Calendar.getInstance();
                try {
                    currentDate = dateToCalendar(EXPANDED_FORMAT.parse(date));
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }

                DatePickerDialog dpd = DatePickerDialog.newInstance(MainActivity.this,
                        currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH),
                        currentDate.get(Calendar.DAY_OF_MONTH));
                dpd.setThemeDark(true);
                dpd.setMinDate(MIN_DATE);
                dpd.setMaxDate(today);
                dpd.vibrate(false);
                dpd.show(getFragmentManager(), "Datepickerdialog");
            }
        });

        // Sliding up panel listener
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_panel_layout);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        slidingPanel.setAnchorPoint(0.42f);
        slidingPanel.setScrollableView(description);
        slidingPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            // Hide FAB while expanded
            @Override
            public void onPanelExpanded(View panel) {
                // Log.i(TAG, "onPanelExpanded");
                fab.hide();
            }

            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            // Show FAB while collapsed
            @Override
            public void onPanelCollapsed(View panel) {
                // Log.i(TAG, "onPanelCollapsed");
                fab.show();
                // Scroll text up so it is hidden when panel is collapsed
                description.smoothScrollTo(0, 0);
            }

            @Override
            public void onPanelAnchored(View panel) {
                fab.show();
            }

            @Override
            public void onPanelHidden(View panel) {
            }

            @Override
            public void onPanelHiddenExecuted(View panel, Interpolator interpolator, int duration) {
            }

            @Override
            public void onPanelShownExecuted(View panel, Interpolator interpolator, int duration) {
            }

            @Override
            public void onPanelExpandedStateY(View panel, boolean reached) {
            }

            @Override
            public void onPanelCollapsedStateY(View panel, boolean reached) {
            }

            @Override
            public void onPanelLayout(View panel, SlidingUpPanelLayout.PanelState state) {
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (imageView.getDrawable() != null) {
                    saveImage(expandedToNumericalDate(date));
                    launchFullImageView(imgUrl, expandedToNumericalDate(date), true);
                }
                // No image available
                else {
                    displayImageNotAvailableToast();
                }
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(MainActivity.this, R.string.toast_fab, Toast.LENGTH_SHORT).show();
                return true;
            }
        });


        final GestureDetector gdt = new GestureDetector(MainActivity.this, new GestureListener());
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            }
        });

        mainView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            }
        });
    } // End onCreate method

    private void displayImageNotAvailableToast() {
        Toast.makeText(MainActivity.this, R.string.toast_no_image, Toast.LENGTH_SHORT).show();
    }

    private void nextDay() {
        Glide.clear(imageView);
        progressBar.setVisibility(View.VISIBLE);

        // Display next day
        date = getNextDay(date);
        dateText.setText(date);

        if (date.equals(today)) {
            tomorrow.setVisibility(View.INVISIBLE);
        }
        if (slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED && fab
                .getVisibility() == View.GONE) {
            fab.show();
        }

        // Set image
        getImageData(date);
    }

    private void previousDay() {
        Glide.clear(imageView);
        progressBar.setVisibility(View.VISIBLE);

        // Display previous day
        date = getPreviousDay(date);
        dateText.setText(date);

        if (tomorrow.getVisibility() == View.INVISIBLE)
            tomorrow.setVisibility(View.VISIBLE);

        if (slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED && fab
                .getVisibility() == View.GONE)
            fab.show();

        // Set image
        getImageData(date);
    }

    /**
     * Calculate the day after the given date
     *
     * @param date formatted date
     *
     * @return the next day after the provided date
     */
    private String getNextDay(String date) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(EXPANDED_FORMAT.parse(date));
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            return EXPANDED_FORMAT.format(calendar.getTime());
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Calculate the day before the given date
     *
     * @param date formatted date
     *
     * @return the previous day before the provided date
     */
    private String getPreviousDay(String date) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(EXPANDED_FORMAT.parse(date));
            calendar.add(Calendar.DAY_OF_YEAR, -1);

            return EXPANDED_FORMAT.format(calendar.getTime());
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Date Methods

    /**
     * @param view
     * @param year
     * @param monthOfYear
     * @param dayOfMonth
     */
    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        imageView.setImageResource(0);
        progressBar.setVisibility(View.VISIBLE);

        // Create Calendar object of selected date
        Calendar pickedDate = new GregorianCalendar(year, monthOfYear, dayOfMonth);

        // Convert date into expanded format
        date = EXPANDED_FORMAT.format(pickedDate.getTime());
        dateText.setText(date);

        // Show/hide right navigation chevron
        if (tomorrow.getVisibility() == View.VISIBLE && date.equals(today)) {
            tomorrow.setVisibility(View.INVISIBLE);
        }
        else if (tomorrow.getVisibility() == View.INVISIBLE && !date.equals(today)) {
            tomorrow.setVisibility(View.VISIBLE);
        }

        // Show progress loading circle
        getImageData(date);
    }

    private String expandedToNumericalDate(String date) {
        // Convert date format to yyyy-mm-dd
        try {
            return NUMERICAL_FORMAT.format(EXPANDED_FORMAT.parse(date));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    private String numericalToExpandedDate(String date) {
        // Convert date format to MMMM dd, yyyy
        try {
            return EXPANDED_FORMAT.format(NUMERICAL_FORMAT.parse(date));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    private String getFullUrl() {
        final String BASE_URL = "http://apod.nasa.gov/apod/ap";
        String shortDate;

        try {
            shortDate = SHORT_FORMAT.format(EXPANDED_FORMAT.parse(date));
            return BASE_URL + shortDate + ".html";
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    // Inflate options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        // Set colour of share icon to white (from black)
        MenuItem menuItem = menu.findItem(R.id.action_share);
        Drawable drawable = menuItem.getIcon();

        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(ContextCompat.getColor(this, R.color.colorWhite), PorterDuff
                    .Mode.SRC_ATOP);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_share:
                shareImage(titleText.getText().toString());
                return true;
            case R.id.action_save:
                saveImage(expandedToNumericalDate(date));
                return true;
            case R.id.action_open_link:
                openLink();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void shareImage(String title) {
        final String IMAGE_DIRECTORY = sharedPref.getString("pref_save_location",
                DEFAULT_IMAGE_DIRECTORY);
        Intent share = new Intent(Intent.ACTION_SEND);

        // Share link if non-image content
        if (tooEarly) {
            displayImageNotAvailableToast();
        }
        else if (imageView.getDrawable() == null) {
            share.setType("text/plain");
            share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            share.putExtra(Intent.EXTRA_SUBJECT, title);
            share.putExtra(Intent.EXTRA_TEXT, getFullUrl());

            startActivity(Intent.createChooser(share, "Share link"));
        }
        // Otherwise share image
        else {
            share.setType("image/jpeg");
            share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ImageActivity.verifyStoragePermissions(this);

            saveImage(expandedToNumericalDate(date));

            String path = IMAGE_DIRECTORY + expandedToNumericalDate(date) + IMAGE_EXT;
            File image = new File(path);
            Uri uri = Uri.fromFile(image);

            share.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(share, "Share image"));
        }
    }

    public void saveImage(String imageDate) {
        // Exit if no image is available
        if (imageView.getDrawable() == null) {
            displayImageNotAvailableToast();
            return;
        }

        final String DATE = imageDate;
        final String IMAGE_DIRECTORY = sharedPref.getString("pref_save_location",
                DEFAULT_IMAGE_DIRECTORY);
        ImageActivity.verifyStoragePermissions(this);

        // Load image with Glide as bitmap
        Glide.with(this).load(imgUrl).asBitmap().diskCacheStrategy(DiskCacheStrategy.SOURCE).into
                (new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                    glideAnimation) {

                File imageDirectory = new File(IMAGE_DIRECTORY);

                if (!imageDirectory.exists()) {
                    imageDirectory.mkdir();
                }
                String filename = DATE + IMAGE_EXT;
                File image = new File(imageDirectory, filename);

                String message = getResources().getString(R.string.toast_save_image) +
                        IMAGE_DIRECTORY + filename;

                // Encode the file as a JPG image.
                FileOutputStream outStream;
                try {
                    outStream = new FileOutputStream(image);
                    resource.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

                    outStream.flush();
                    outStream.close();

                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
                catch (FileNotFoundException e) {
                    Toast.makeText(MainActivity.this, R.string.error_saving, Toast.LENGTH_SHORT)
                            .show();
                }
                catch (IOException e) {
                    Toast.makeText(MainActivity.this, R.string.error_saving + image.getPath(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Sliding up panel listener
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_panel_layout);

        if (slidingPanel != null && (slidingPanel.getPanelState() == SlidingUpPanelLayout
                .PanelState.EXPANDED)) {
            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
        else {
            super.onBackPressed();
        }
    }

    private void onJsonResponse(JSONObject response) {
        final String IMAGE_TYPE = "image";

        boolean prefHd;
        boolean prefCopyright;
        String copyright;
        String explanation;
        String mediaType;
        String hdUrl;
        String title;

        try {
            // final String numericalDate = response.getString("date");
            explanation = response.getString("explanation");
            mediaType = response.getString("media_type");
            sdUrl = response.getString("url");
            title = response.getString("title");

            hdUrl = "";
            prefHd = sharedPref.getString("image_quality", "").equals("1");
            prefCopyright = sharedPref.getBoolean("pref_display_credit", false);

            // Check if HD image URL is included in response
            if (response.has("hdurl")) {
                hdUrl = response.getString("hdurl");
            }

            // Add copyright credits to end of description if setting allows it
            if (prefCopyright && response.has("copyright")) {
                copyright = response.getString("copyright");
                explanation += getResources().getString(R.string.title_credits) + copyright;
            }

            // Set image url depending on user preference and image availability
            if (prefHd && !hdUrl.equals("")) {
                imgUrl = hdUrl;
            }
            else {
                imgUrl = sdUrl;
            }

            // Set text
            titleText.setText(title);
            description.setText(explanation);

            if (mediaType.equals(IMAGE_TYPE)) {
                Glide.with(MainActivity.this).load(sdUrl) // Load from URL
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
                        .centerCrop()
                        //.dontAnimate() // No cross-fade
                        .skipMemoryCache(true) // Use disk cache only
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model,
                                                       Target<GlideDrawable> target, boolean
                                                               isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model,
                                                           Target<GlideDrawable> target, boolean
                                                                   isFromMemoryCache, boolean
                                                                   isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }

                        }).into(imageView);
            }
            else {
                openNonImageContent(expandedToNumericalDate(date), sdUrl);
            }
            tooEarly = false;
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getImageData(String date) {
        // Parse date
        String apiDate = expandedToNumericalDate(date);
        RequestQueue queue;
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        queue = new RequestQueue(cache, network);
        // Start the queue
        queue.start();

        String url = "https://api.nasa.gov/planetary/apod?api_key=***REMOVED***" + "&date=" + apiDate;
        Log.i("URL", url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                onJsonResponse(response);
            }
        }, new Response.ErrorListener() {
            // Handle Volley errors
            @Override
            public void onErrorResponse(VolleyError error) {
                int messageId;
                progressBar.setVisibility(View.GONE);

                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    messageId = R.string.error_internet;
                }
                else if (error instanceof AuthFailureError) {
                    messageId = R.string.error_auth;
                }
                else if (error instanceof ServerError) {
                    messageId = R.string.error_server;
                    tooEarly = true;
                }
                else if (error instanceof NetworkError) {
                    messageId = R.string.error_network;
                }
                else if (error instanceof ParseError) {
                    messageId = R.string.error_parse;
                }
                else {
                    messageId = R.string.error_general;
                }

                // Display long toast message
                Toast.makeText(MainActivity.this, messageId, Toast.LENGTH_SHORT).show();
            }

        }) {
            // Set caching
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                    if (cacheEntry == null) {
                        cacheEntry = new Cache.Entry();
                    }

                    final long cacheHitButRefreshed = 3 * 60 * 1000; // in 3 minutes cache will
                    // be hit, but also refreshed on background
                    final long cacheExpired = 24 * 60 * 60 * 1000; // in 24 hours this cache
                    // entry expires completely
                    long now = System.currentTimeMillis();
                    final long softExpire = now + cacheHitButRefreshed;
                    final long ttl = now + cacheExpired;

                    cacheEntry.data = response.data;
                    cacheEntry.softTtl = softExpire;
                    cacheEntry.ttl = ttl;
                    String headerValue;

                    headerValue = response.headers.get("Date");
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }

                    headerValue = response.headers.get("Last-Modified");
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }

                    cacheEntry.responseHeaders = response.headers;
                    final String jsonString = new String(response.data, HttpHeaderParser
                            .parseCharset(response.headers));

                    return Response.success(new JSONObject(jsonString), cacheEntry);
                }
                catch (UnsupportedEncodingException e) {
                    return Response.error(new ParseError(e));
                }
                catch (JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }

            @Override
            protected void deliverResponse(JSONObject response) {
                super.deliverResponse(response);
            }

            @Override
            public void deliverError(VolleyError error) {
                super.deliverError(error);
            }
        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy
                .DEFAULT_BACKOFF_MULT));

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }

    /**
     * Launch activity to display image in fullscreen
     *
     * @param url URL of the image
     */
    public void launchFullImageView(String url, String numericalDate, boolean setWallpaper) {
        Intent intent = new Intent(MainActivity.this, ImageActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("date", numericalDate);
        intent.putExtra("wallpaper", setWallpaper);
        startActivity(intent);
    }

    private void openLink() {
        String url = getFullUrl();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void openNonImageContent(String date, String url) {
        final String uri = url;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = String.format(getResources().getString(R.string.dialog_browser_desc),
                numericalToExpandedDate(date));

        imageView.setImageResource(0);
        progressBar.setVisibility(View.GONE);

        builder.setTitle(R.string.dialog_browser_title);
        builder.setMessage(message);

        builder.setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Open Link in browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(browserIntent);
            }
        });

        builder.setNegativeButton(R.string.action_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    /**
     * http://stackoverflow.com/questions/4098198/adding-fling-gesture-to-an-image-view-android
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final int SWIPE_MIN_DISTANCE = 120;
        private final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Right to left
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) >
                    SWIPE_THRESHOLD_VELOCITY) {
                // Prevent user from navigation to future days
                if (!date.equals(today))
                    nextDay();
                return false;
            }
            // Left to right
            else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) >
                    SWIPE_THRESHOLD_VELOCITY) {
                previousDay();
                return false;
            }
            return false;
        }

        // A confirmed single-tap event has occurred.  Only called when the detector has
        // determined that the first tap stands alone, and is not part of a double tap.
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (tooEarly) {
                Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
            }
            else {
                if (imageView.getDrawable() == null) {
                    openNonImageContent(expandedToNumericalDate(date), sdUrl);
                }
                else {
                    launchFullImageView(sdUrl, expandedToNumericalDate(date), false);
                }
            }
            return false;
        }

        // Touch has been long enough to indicate a long press.
        // Does not indicate motion is complete yet (no up event necessarily)
        @Override
        public void onLongPress(MotionEvent e) {
            if (imageView.getDrawable() == null) {
                Toast.makeText(MainActivity.this, R.string.toast_view_external, Toast
                        .LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(MainActivity.this, R.string.toast_view_image, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
}