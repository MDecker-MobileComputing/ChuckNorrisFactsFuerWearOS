package de.mide.wear.chucknorrisfacts;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Activity fetches "Chuck Norris Facts" (short jokes) from Web-API and displays them.
 * <br><br>
 *
 * Alternate approach for refresh of joke might be
 * {@link android.support.v4.widget.SwipeRefreshLayout}, see
 * <a href="http://www.vogella.com/tutorials/SwipeToRefresh/article.html">this tutorial</a>.
 * <br><br>
 *
 * This project is licensed under the terms of the BSD 3-Clause License.
 * <br><br>
 *
 * The author of this app is neither related to Chuck Norris nor to the
 * developer/provider of the Web-API <a href="http://www.icndb.com/api/">icndb.com</a>.
 */
public class MainActivity extends WearableActivity
                          implements View.OnClickListener {

    /** Kennzeichnungs-String ("Schildchen") für alle Log-Nachrichten in dieser App. */
    public static final String TAG4LOGGING = "ChuckNorris";

    /**
     * Es wird die URL für eine ungesichterte HTTP-Verbindung ("http://" statt "https://"
     * verwendet), weil es bei Ausführung der App im Emulator sonst wahrscheinlich zu
     * einer Exception wegen einem Fehler bei der Validierung der Certificate Chain kommt.
     * Da WearOS/Android in neueren Versionen bei ungesicherten HTTP-Verbindungen eine Exception
     * wirft, muss die Domain dieser URL in eine Whitelist eingetragen werden, siehe Datei
     * <code></code>res/xml/network_security_config.xml</code>, die im <code>application</code>-Tag
     * in der Manifest-Datei referenziert wird.
     */
    protected static final String WEB_API_URL = "http://api.icndb.com/jokes/random?exclude=[explicit]";


    /** UI-Element zur Darstellung von Ergebnis und Fehlermeldungen. */
    protected TextView _ergebnisTextView = null;

    /** UI-Element für Text "Text oben berühren, um anderen Chuck Norris Fact zu laden";
     *  das Element wird während des Ladevorgangs auf unsichtbar geschaltet. */
    protected TextView _bedienungshinweisTextview = null;

    /** Flag, um mehrere gleichzeitige Ladevorgänge zu verhindern. */
    protected boolean _ladevorgangLaueft = false;


    /**
     * Lifecycle-Methode. Nachdem alle Initialisierungen vorgenommen sind
     * wird gleich ein Witz geladen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _ergebnisTextView = findViewById(R.id.text);
        _ergebnisTextView.setOnClickListener(this);

        _bedienungshinweisTextview = findViewById(R.id.bedienungshinweisTextview);

        setAmbientEnabled(); // Enables Always-on

        ladeWitz();
    }


    /**
     * Event-Handler-Methode, lädt neuen Witz (wenn nicht bereits ein anderer
     * Ladevorgang läuft).
     *
     * @param view  Element, das Event ausgelöst hat (nämlich {@link #_ergebnisTextView}.
     */
    @Override
    public void onClick(View view) {

        if (_ladevorgangLaueft == true) {
            Log.i(TAG4LOGGING, "Es läuft schon ein Ladevorgang.");
            return;
        }

        ladeWitz();
    }
    
    
    /**
     * Erzeugt eine Instanz von {@link MeinAsyncTask} und startet sie.
     * Dadurch wird der Zugriff auf die Web-API in einem Worker-Thread
     * durchgeführt.
     */
    protected void ladeWitz() {

        _ladevorgangLaueft = true;

        MeinAsyncTask mat = new MeinAsyncTask();
        mat.execute();

        //Als Einzeiler: new MeinAsyncTask().execute();
    }


    /* *************************************** */
    /* ********* Start innere Klasse ********* */
    /* *************************************** */

    /**
     * Ein Objekt dieser Klasse führt den Netzwerk-Zugriff für den Aufruf
     * der Web-API und das anschließende Parsen des als Antwort erhaltenen
     * JSON-Dokuments in einem Worker-Thread (Hintergrund-Thread) durch.
     */
    protected class MeinAsyncTask extends AsyncTask<Void,Void,String> {

        /**
         * Diese Methode wird vor der Methode {@link MeinAsyncTask#doInBackground(Void...)}
         * ausgeführt, und zwar im Main-Thread. In ihr können deshalb Änderungen an der UI
         * vorgenommen werden. Die Methode ändert den vom {@link TextView}-Element angezeigten
         * Text auf eine Meldung, die besagt, dass der Ladevorgang läuft.
         * Außerdem wird das UI-Element {@link #_bedienungshinweisTextview} auf "unsichtbar"
         * geschaltet (es wird in {@link MeinAsyncTask#onPostExecute(String)} wieder
         * auf "sichtbar" geschaltet).
         */
        @Override
        protected void onPreExecute() {

            String text = getString( R.string.loading );
            _ergebnisTextView.setText( text );

            _bedienungshinweisTextview.setVisibility(View.INVISIBLE);
        }


        /**
         * Diese Methode führt den Zugriff auf die Web-API und das Extrahieren
         * des Witz aus der empfangenen JSON-Datei in einem Worker-Thread (also
         * nicht im Main-Thread) durch.
         *
         * @param voids  Dies Methode benötigt keine Parameter.
         *
         * @return  String mit dem Witz (Chuck Norris Fact).
         */
        @Override
        public String doInBackground(Void... voids) {

            try {

                String jsonString = holeWitz();

                String witzString = extrahiereWitzAusJson( jsonString );

                return witzString;

            } catch (Exception ex) {

                Log.e(TAG4LOGGING, "Exception: " + ex);
                return "Error: " + ex.getMessage();
            }
        }

        /**
         * Diese Methode wird unmittelbar nach Beendigung der Methode {@link #doInBackground(Void...)}
         * angezeigt. Sie wird im Main-Thread ausgeführt und zeigt das Ergebnis dieser Methode auf
         * dem {@link TextView}-Element an.
         * Außerdem wird das UI-Element {@link #_bedienungshinweisTextview} auf "sichtbar"
         * geschaltet (es wurde in {@link MeinAsyncTask#onPreExecute()} auf "unsichtbar"
         * geschaltet).
         *
         * @param resultString  Der String mit dem anzuzeigenden Text (Witz, wenn kein Fehler
         *                      aufgetreten ist, oder eine Fehlermeldung).
         */
        @Override
        protected void onPostExecute(String resultString) {

            _ergebnisTextView.setText( resultString );

            _bedienungshinweisTextview.setVisibility( View.VISIBLE );

            _ladevorgangLaueft = false;
        }
    };

    /* *************************************** */
    /* ********* Ende innere Klasse  ********* */
    /* *************************************** */


    /**
     * Methode mit HTTP-Zugriff, muss in Hintergrund-Thread (Worker-Thread) ausgeführt werden!
     *
     * @return HTTP-Response-String oder leerer String bei Fehler (aber nicht <i>null</i>).
     *
     * @throws Exception  Fehler beim Internet-Zugriff.
     */
    protected String holeWitz() throws Exception {

        URL               url                  = null;
        HttpURLConnection conn                 = null;
        String            httpErgebnisDokument = "";

        url  = new URL(WEB_API_URL);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET"); // Eigentlich nicht nötig, weil "GET" Default-Wert ist.

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {

            String errorMessage = getString(R.string.http_error) + conn.getResponseMessage();
            throw new Exception( errorMessage );

        } else {

            InputStream is        = conn.getInputStream();
            InputStreamReader ris = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(ris);

            // JSON-Dokument zeilenweise einlesen
            String zeile = "";
            while ( (zeile = reader.readLine()) != null) {
                httpErgebnisDokument += zeile;
            }
        }

        return httpErgebnisDokument;
    }


    /**
     * Methode für parsen der JSON-Antwort von der Web-API, um den eigentlichen
     * Witz zu erhalten.
     *
     * @param jsonString  Komplettes JSON-Dokument, das von der Web-API geliefert wurde
     *
     * @return  String mit dem Kurzwitz (Chuck Norris Fact).
     *
     * @throws JSONException  Fehler beim Parsen des JSON-Objekts; wird geworfen, wenn
     *                        für einen bestimmten Key kein Wert des entsprechenden
     *                        Datentypes gefunden wird.
     */
    protected String extrahiereWitzAusJson(String jsonString) throws JSONException {

        JSONObject hauptObjekt = new JSONObject(jsonString);

        String typeString = hauptObjekt.getString("type");

        if (typeString.equalsIgnoreCase("success") == false) {
            throw new JSONException( getString( R.string.status_not_success) + typeString );
        }

        // Unterobjekt mit dem eigentlichen Witz holen.
        JSONObject unterObjekt = hauptObjekt.getJSONObject("value");

        int    witzID     = unterObjekt.getInt("id");
        String witzString = unterObjekt.getString("joke");
        // unterObjekt hat unter dem Key "categories" noch einen Array mit den
        // gewählten Kategorien (z.B. "nerdy" oder "explicit"), aber das lesen
        // wir nicht aus.

        Log.i(TAG4LOGGING, "Witz " + witzID + ": " + witzString);

        // ggf. Anführungszeichen ersetzen
        witzString = witzString.replace("&quot;", "\"");

        return witzString;
    }

}
