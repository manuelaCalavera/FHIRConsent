package ch.black_book.fhirconsent;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ch.black_book.fhirconsent.LabelReader.PatientRecord;

import static ch.black_book.fhirconsent.MainActivity.logTag;

public class CheckInfoActivity extends AppCompatActivity {

    private PatientRecord record;
    EditText lastName;
    EditText firstName;
    EditText dobPicker;
    EditText patientCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_info);


        Intent intent = getIntent();
        record = new PatientRecord();
        record.FirstName = intent.getExtras().getString("FIRST_NAME");
        record.LastName = intent.getStringExtra("LAST_NAME");
        record.Code = intent.getStringExtra("CODE");
        record.DateString = intent.getStringExtra("DOB");

        lastName = (EditText) findViewById(R.id.last_name);
        lastName.setText(record.LastName, TextView.BufferType.EDITABLE);

        firstName = (EditText) findViewById(R.id.first_name);
        firstName.setText(record.FirstName, TextView.BufferType.EDITABLE);


        final Calendar dobCalendar = Calendar.getInstance();
        dobPicker= (EditText) findViewById(R.id.dob_picker);
        dobPicker.setText(record.DateString);
        final SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        Date dob = null;
        try {
            dob = formatter.parse(record.DateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        dobCalendar.setTime(dob);

        dobPicker.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //To show current date in the datepicker

                int dobYear=dobCalendar.get(Calendar.YEAR);
                int dobMonth=dobCalendar.get(Calendar.MONTH);
                int dobDay=dobCalendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog mDatePicker=new DatePickerDialog(CheckInfoActivity.this, new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker datepicker, int selectedyear, int selectedmonth, int selectedday) {
                        // TODO Auto-generated method stub
                        /*      Your code   to get date and time    */
                        dobCalendar.set(selectedyear, selectedmonth, selectedday);
                        Log.d(logTag, "selected date:"+formatter.format(dobCalendar.getTime()));
                        dobPicker.setText(formatter.format(dobCalendar.getTime()));
                        //dobPicker.setText(selectedday +"."+selectedmonth+1 +"."+selectedyear);
                    }
                },dobYear, dobMonth, dobDay);
                //mDatePicker.setTitle("Select date");
                mDatePicker.show();  }
        });

        patientCode = (EditText) findViewById(R.id.patient_code);
        patientCode.setText(record.Code, TextView.BufferType.EDITABLE);


        Button confirmButton = (Button) findViewById(R.id.confirm_info);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent data = new Intent();

                data.putExtra("FIRST_NAME", firstName.getText().toString());
                data.putExtra("LAST_NAME", lastName.getText().toString());
                data.putExtra("CODE", patientCode.getText().toString());
                data.putExtra("DOB", dobPicker.getText().toString());

                setResult(RESULT_OK, data);
                finish();
            }
        });

    }
}
