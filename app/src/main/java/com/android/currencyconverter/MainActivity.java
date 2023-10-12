package com.android.currencyconverter;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity
        extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static double exchangeRate;
    private TextView fromCurrency;
    private TextView toCurrency;
    private TextView exchangeRateText;
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private JSONObject apiResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up fullscreen and keep screen on
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Initialize JSON Object for API response
        apiResponse = new JSONObject();

        // Initialize UI elements
        fromCurrency = findViewById(R.id.fromCurrency);
        toCurrency = findViewById(R.id.toCurrency);
        exchangeRateText = findViewById(R.id.exchangeRateText);

        // Fetch exchange rate data
        getExchangeRate();
        // Set up numeric keyboard
        setupKeyboard();
        // Set up spinners
        setupSpinners();
        // Set up currency formatting
        currencyFormatter();
    }
    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.currencies,
                R.layout.spinner_selected_item
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_text_color);

        fromSpinner = findViewById(R.id.fromSpinner);
        fromSpinner.setOnItemSelectedListener(this);
        fromSpinner.setAdapter(adapter);
        fromSpinner.setSelection(13);

        toSpinner = findViewById(R.id.toSpinner);
        toSpinner.setOnItemSelectedListener(this);
        toSpinner.setAdapter(adapter);
        toSpinner.setSelection(17);
        }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
        // Update exchangeRate when a new item is selected on the Spinners
        setExchangeRate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    private void setupKeyboard() {

        // Define the mapping of button IDs to resource names
        Map<Integer, String> buttonImageMap = new HashMap<>();
        buttonImageMap.put(R.id.button0, "0");
        buttonImageMap.put(R.id.button1, "1");
        buttonImageMap.put(R.id.button2, "2");
        buttonImageMap.put(R.id.button3, "3");
        buttonImageMap.put(R.id.button4, "4");
        buttonImageMap.put(R.id.button5, "5");
        buttonImageMap.put(R.id.button6, "6");
        buttonImageMap.put(R.id.button7, "7");
        buttonImageMap.put(R.id.button8, "8");
        buttonImageMap.put(R.id.button9, "9");

        // Set a common listener for all digit buttons
        View.OnClickListener digitClickListener = view -> {
            ImageButton digitButton = (ImageButton) view;
            String digit = buttonImageMap.get(digitButton.getId());

            String currentText = fromCurrency.getText().toString();
            if (currentText.length() != 0) {
                String text = currentText + digit;
                fromCurrency.setText(text);
            } else {
                fromCurrency.setText(digit);
            }
        };

        // Assign the common listener to each digit button
        for (int buttonId : buttonImageMap.keySet()) {
            findViewById(buttonId).setOnClickListener(digitClickListener);
        }

        // Assign listeners to backspace and interchange buttons
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        ImageButton buttonInterchange = findViewById(R.id.interchange);

        buttonInterchange.setOnClickListener(view -> {
            int fromSelection = fromSpinner.getSelectedItemPosition();
            fromSpinner.setSelection(toSpinner.getSelectedItemPosition());
            toSpinner.setSelection(fromSelection);
            setExchangeRate();
        });

        buttonBack.setOnClickListener(view -> {
            if (fromCurrency.getText().toString().length() != 0) {
                String text = fromCurrency.getText().toString();
                String newText = text.substring(0, text.length()-1);
                fromCurrency.setText(newText);
            }
        });

    }

    private void setExchangeRate() {
        // Get values from Spinners
        String fromString = fromSpinner.getSelectedItem().toString();
        String toString = toSpinner.getSelectedItem().toString();

        try {
            if (apiResponse.length() != 0) {
                // Update exchangeRate
                double fromValue = apiResponse.getDouble(fromString);
                double toValue = apiResponse.getDouble(toString);
                MainActivity.exchangeRate = toValue / fromValue;

                // Update exchangeRateText TextView
                String text = "x " + df.format(MainActivity.exchangeRate);  // TODO: thousand separators!
                exchangeRateText.setText(text);
            }
        } catch (JSONException e) {
        // Handle JSON error
            String text = "Error loading JSON";
            fromCurrency.setText(text);
        }

        // Update toCurrency
        updateToValue();
    }
    private void getExchangeRate() {
        // Connect to database

        String databaseUrl = "https://currency-converter-76eeb-default-rtdb.europe-west1.firebasedatabase.app/";
        DatabaseReference databaseReference = FirebaseDatabase.getInstance(databaseUrl).getReference();
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check if the data snapshot has any data
                if (dataSnapshot.exists()) {
                    // Iterate through the children of the snapshot
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        try {
                            apiResponse.put(Objects.requireNonNull(childSnapshot.getKey()), childSnapshot.getValue());
                        } catch (JSONException e) {
                            // Handle JSON error
                            String text = "Error loading JSON";
                            fromCurrency.setText(text);
                        }
                    }
                    // Update exchangeRate
                    setExchangeRate();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
                String text = "Error loading database";
                fromCurrency.setText(text);
            }
        });
    }

    private void updateToValue() {
        String fromString = fromCurrency.getText().toString();

        // Remove commas for proper parsing
        fromString = fromString.replace(",", "");
        if (fromString.isEmpty()) {
            // Clear toCurrency if fromCurrency is empty
            toCurrency.setText("");
            return;
        }
        try {
            // Parse fromCurrency's value as a double, multiply with exchange rate
            double fromValue = Double.parseDouble(fromString);
            double toValue = (fromValue * MainActivity.exchangeRate);

            // Format calculated value
            DecimalFormat formatter = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
            symbols.setCurrencySymbol("");
            formatter.setDecimalFormatSymbols(symbols);

            // Set formatted toValue to the toCurrency TextView
            toCurrency.setText(formatter.format(toValue));
        } catch (NumberFormatException e) {
            // Handle invalid input (non-numeric characters)
            toCurrency.setText("");
        }
    }

    private void currencyFormatter() {
        TextWatcher mTextWatcher = new TextWatcher() {

            public void textFormatter(TextView textView) {
                // Format text with commas
                String editTextString = textView.getText().toString();
                editTextString = editTextString.replace(",", "");
                int editTextInt = Integer.parseInt(editTextString);
                String formattedText = String.format(Locale.ENGLISH, "%,d", editTextInt);
                textView.setText(formattedText);  // TODO: need to get decimals into input
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing to do before the text change
            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Update toCurrency
                updateToValue();
                if (fromCurrency.getText().toString().length() != 0) {
                    fromCurrency.removeTextChangedListener(this);
                    textFormatter(fromCurrency);
                    fromCurrency.addTextChangedListener(this);


                    if (toCurrency.getText().length() == 16) {
                        int currentLength = fromCurrency.getText().length();
                        fromCurrency.setFilters(new InputFilter[]{new InputFilter.LengthFilter(currentLength)});
                    } else {
                        fromCurrency.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
                // Nothing to do after the text change
            }
        };
        fromCurrency.addTextChangedListener(mTextWatcher);
    }

}