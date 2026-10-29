package com.example.cartly;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

// Plans a shopping: optional description, day and time. Tells PlannedFragment through RESULT when saved.
public class NewShoppingDialogFragment extends DialogFragment {
    static final String RESULT = "shopping_planned";

    private String date;  // "yyyy-MM-dd"
    private String time;  // "HH:mm"
    private Button dateButton;
    private Button timeButton;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            date = savedInstanceState.getString("date");
            time = savedInstanceState.getString("time");
        } else {
            // the next full hour, which may already be tomorrow
            Calendar next = Calendar.getInstance();
            next.add(Calendar.HOUR_OF_DAY, 1);
            next.set(Calendar.MINUTE, 0);
            date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(next.getTime());
            time = new SimpleDateFormat("HH:mm", Locale.ROOT).format(next.getTime());
        }

        // not getLayoutInflater(): inside onCreateDialog it would call onCreateDialog again
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_shopping, null);
        EditText description = view.findViewById(R.id.input_description);
        dateButton = view.findViewById(R.id.button_date);
        timeButton = view.findViewById(R.id.button_time);
        dateButton.setOnClickListener(v -> pickDate());
        timeButton.setOnClickListener(v -> pickTime());
        updateButtons();
        reattachPickers();

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.new_shopping)
                .setView(view)
                .setPositiveButton(R.string.save, null)  // set below: a time in the past must not close the dialog
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // ISO strings compare like the moments they stand for
            if ((date + " " + time).compareTo(DBManager.today() + " " + DBManager.now()) <= 0) {
                Toast.makeText(requireContext(), R.string.time_in_past, Toast.LENGTH_SHORT).show();
                return;
            }
            save(description.getText().toString().trim());
            dismiss();
        }));
        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("date", date);
        outState.putString("time", time);
    }

    private void save(String description) {
        DBManager db = new DBManager(requireContext());
        DBManager.Shopping shopping = db.findShopping((int) db.addShopping(description, date, time, DBManager.PLANNED));
        if (shopping != null)
            NotificationReceiver.schedule(requireContext(), shopping);
        getParentFragmentManager().setFragmentResult(RESULT, new Bundle());
    }

    private void updateButtons() {
        dateButton.setText(Ui.formatDate(requireContext(), date));
        timeButton.setText(time);
    }

    private void pickDate() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.pick_date)
                .setSelection(utcMillis(date))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.now())  // no days in the past
                        .build())
                .build();
        listen(picker);
        picker.show(getChildFragmentManager(), "date");
    }

    private void pickTime() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(Integer.parseInt(time.substring(0, 2)))
                .setMinute(Integer.parseInt(time.substring(3)))
                .setTitleText(R.string.pick_time)
                .build();
        listen(picker);
        picker.show(getChildFragmentManager(), "time");
    }

    private void listen(MaterialDatePicker<Long> picker) {
        picker.addOnPositiveButtonClickListener(selection -> {
            date = utcFormat().format(selection);
            updateButtons();
        });
    }

    private void listen(MaterialTimePicker picker) {
        picker.addOnPositiveButtonClickListener(v -> {
            time = String.format(Locale.ROOT, "%02d:%02d", picker.getHour(), picker.getMinute());
            updateButtons();
        });
    }

    // a picker that was open before a rotation comes back without its listener
    @SuppressWarnings("unchecked")
    private void reattachPickers() {
        Fragment datePicker = getChildFragmentManager().findFragmentByTag("date");
        if (datePicker instanceof MaterialDatePicker)
            listen((MaterialDatePicker<Long>) datePicker);
        Fragment timePicker = getChildFragmentManager().findFragmentByTag("time");
        if (timePicker instanceof MaterialTimePicker)
            listen((MaterialTimePicker) timePicker);
    }

    // MaterialDatePicker works with midnight UTC
    private static SimpleDateFormat utcFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    private static Long utcMillis(String isoDate) {
        try {
            return utcFormat().parse(isoDate).getTime();
        } catch (ParseException e) {
            return MaterialDatePicker.todayInUtcMilliseconds();
        }
    }
}
