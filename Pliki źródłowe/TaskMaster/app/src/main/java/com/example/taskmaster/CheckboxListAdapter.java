package com.example.taskmaster;

import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CheckboxListAdapter extends RecyclerView.Adapter<CheckboxListAdapter.ViewHolder> {

    public interface LastTaskRemovedListener {
        void onLastTaskRemoved();
    }
    private LastTaskRemovedListener lastTaskRemovedListener;
    public void setLastTaskRemovedListener(LastTaskRemovedListener listener) {
        this.lastTaskRemovedListener = listener;
    }
    private final ActivityResultLauncher<String> attachmentContentLauncher;
//    private int currentAdapterPosition = -1;
//    protected int getAdapterPosition() {
//        return this.currentAdapterPosition;
//    }

    protected ChipGroup getAttachmentChipGroup() {
        return this.activeAttachmentChipGroup;
    }
    private ChipGroup activeAttachmentChipGroup;
    private final ArrayList<Uri> currentAttachments = new ArrayList<>();

    private final Context context;
    private final ArrayList<Task> checkboxList;
    private final int[] priorityColors = {
            R.color.priority1,
            R.color.priority2,
            R.color.priority3,
            R.color.priority4,
            R.color.priority5,
            R.color.priority6,
            R.color.priority7,
            R.color.priority8,
            R.color.priority9,
            R.color.priority10,
    };

    public CheckboxListAdapter(Context context, ArrayList<Task> checkboxList, ActivityResultLauncher<String> attachmentContentLauncher) {
        this.context = context;
        this.checkboxList = checkboxList;
        this.attachmentContentLauncher = attachmentContentLauncher;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == checkboxList.size()) ? R.layout.add_task_button : R.layout.task_item;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        /* Nadmuchaj element task_item (checkbox dołączany do recyclerView) */
        LayoutInflater taskInflater = LayoutInflater.from(context);
        View taskItem = taskInflater.inflate(viewType, parent, false);
        return new ViewHolder(taskItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if(position != checkboxList.size()) {
            /* Przypisanie konkretnemu elementowi właściwych danych z checkboxList */
            AppCompatCheckBox newTask_checkBox = holder.taskCheckbox;
            TextView newTask_textView = holder.taskText;
            TextView newTask_priority = holder.taskPriority;

            String taskName = getItem(position).getName();
            int priority = getItem(position).getPriority();

            newTask_checkBox.setTag(taskName);
            newTask_textView.setText(taskName);
            newTask_priority.setText(String.valueOf(priority));
            newTask_priority.getBackground().setColorFilter(ContextCompat.getColor(context, priorityColors[priority - 1]), PorterDuff.Mode.SRC_IN);

            /* Przekreślenie treści zadania i zaznaczenie checkboxa, jeśli zostało już wykonane */
            if (checkboxList.get(position).getStatus().equals(context.getString(R.string.done))) {
                newTask_checkBox.setChecked(true);
                newTask_textView.setPaintFlags(newTask_textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                newTask_checkBox.setChecked(false);
                newTask_textView.setPaintFlags(newTask_textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }
    }

    private Task getItem(int position) {
        return checkboxList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return checkboxList.size() + 1; // FAB dodający zadanie jest traktowany jako dodatkowy element
    }

    /* Własna implementacja klasy ViewHolder, ustawiająca nasłuchiwacza kliknięcia w zadanie */
    protected class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatCheckBox taskCheckbox;
        private final TextView taskText;
        private final TextView taskPriority;
        private final LinearLayout taskItem;
        private int adapterPosition;
        private Task task;
        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            FloatingActionButton addTaskButton = itemView.findViewById(R.id.floatingActionButton_addTask);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            taskText = itemView.findViewById(R.id.taskName);
            taskPriority = itemView.findViewById(R.id.taskPriority);
            Button deleteTask = itemView.findViewById(R.id.deleteTask);
            taskItem = itemView.findViewById(R.id.taskItem);

            // Sprawdź, czy element jest zwykłym elementem, czy też przyciskiem dodającym zadanie
            if(addTaskButton != null) {
                // Przycisk dodający zadanie
                addTaskButton.setOnClickListener(view -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_YEAR, 7);
                    checkboxList.add(checkboxList.size(), new Task(context));
                    notifyItemInserted(checkboxList.size());
                });
            }
            else {
                // Zwykły element
                /* Pokaż okno dialogowe z konfiguracją zadania po kliknięciu w cały element */
                taskItem.setOnClickListener(view -> {
                    currentAttachments.clear();

                    adapterPosition = getAdapterPosition();
                    task = checkboxList.get(adapterPosition);

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.task_edit_dialog_title);

                    LayoutInflater dialogBodyInflater = LayoutInflater.from(context);
                    View dialogBodyView = dialogBodyInflater.inflate(R.layout.edit_task_dialog_body, taskItem, false);
                    builder.setView(dialogBodyView);

                    EditText editText_editTaskDialog_taskName = dialogBodyView.findViewById(R.id.editText_editTaskDialog_taskName);
                    NumberPicker numberPicker_editTaskDialog_taskPriority = dialogBodyView.findViewById(R.id.numberPicker_editTaskDialog_taskPriority);
                    TextView textView_editTaskDialog_deadlineDate = dialogBodyView.findViewById(R.id.textView_editTaskDialog_deadlineDate);
                    FloatingActionButton floatingActionButton_editTaskDialog_setDeadline = dialogBodyView.findViewById(R.id.floatingActionButton_editTaskDialog_setDeadline);
                    SwitchCompat switch_editTaskDialog_setReminder = dialogBodyView.findViewById(R.id.switch_editTaskDialog_setReminder);
                    FloatingActionButton floatingActionButton_editTaskDialog_addAttachment = dialogBodyView.findViewById(R.id.floatingActionButton_editTaskDialog_addAttachment);
                    ChipGroup chipGroup_editTaskDialog_attachmentList = dialogBodyView.findViewById(R.id.chipGroup_editTaskDialog_attachmentList);
                    activeAttachmentChipGroup = chipGroup_editTaskDialog_attachmentList;

                    // Ustawienie ostatecznego terminu wykonania zadania
                    final Calendar calendarDeadline = Calendar.getInstance();
                    final Calendar calendarToday = Calendar.getInstance();
                    calendarDeadline.setTime(task.getDeadline());
                    updateDeadlineDateText(calendarDeadline, textView_editTaskDialog_deadlineDate);

                    DatePickerDialog.OnDateSetListener date = (view1, year, month, day) -> {
                        calendarDeadline.set(Calendar.YEAR, year);
                        calendarDeadline.set(Calendar.MONTH, month);
                        calendarDeadline.set(Calendar.DAY_OF_MONTH, day);
                        task.setDeadline(calendarDeadline.getTime());
                        notifyItemChanged(adapterPosition);
                        // notifyItemChanged działa dopiero po zamknięciu dialogu, dlatego należy ręcznie zaktualizować datę w dialogu
                        updateDeadlineDateText(calendarDeadline, textView_editTaskDialog_deadlineDate);
                    };
                    floatingActionButton_editTaskDialog_setDeadline.setOnClickListener(fab -> {
                        DatePickerDialog dpd = new DatePickerDialog(
                                context,
                                date,
                                calendarDeadline.get(Calendar.YEAR),
                                calendarDeadline.get(Calendar.MONTH),
                                calendarDeadline.get(Calendar.DAY_OF_MONTH)
                        );
                        dpd.getDatePicker().setMinDate(calendarToday.getTimeInMillis());
                        dpd.show();
                    });

                    /* Dodawanie załączników */
                    // Generuj obecne załączniki
                    if (chipGroup_editTaskDialog_attachmentList != null) {
                        for (int attachmentNo = 0; attachmentNo < task.getAttachments().size(); attachmentNo++) {
                            addAttachmentChip(chipGroup_editTaskDialog_attachmentList, task.getAttachments().get(attachmentNo));
                        }
                    }
                    // Dodaj nowy załącznik
                    floatingActionButton_editTaskDialog_addAttachment.setOnClickListener(fab -> {
                        // Użycie callbacku w celu przekazania akcji do Fragment_checkboxList
                        // Niestety, prościej się nie da, ponieważ wybór pliku musi odbyć się w klasie rozszerzającej Activity lub Fragment...
                        attachmentContentLauncher.launch("*/*");
                    });

                    // Przyciski OK/Anuluj
                    builder.setPositiveButton(R.string.ok, (dialog, id) -> {
                        // Odczytaj ustawienia i odpowiednio zaktualizuj model
                        if (editText_editTaskDialog_taskName != null) {
                            String taskName = editText_editTaskDialog_taskName.getText().toString();
                            task.setName(taskName);
                        }
                        if (numberPicker_editTaskDialog_taskPriority != null) {
                            int taskPriority = numberPicker_editTaskDialog_taskPriority.getValue();
                            task.setPriority(taskPriority);
                        }
                        if (switch_editTaskDialog_setReminder != null) {
                            boolean setReminder = switch_editTaskDialog_setReminder.isChecked();
                            task.setReminder(setReminder);
                            if (setReminder) {
                                buildTaskNotification(task);
                            }
                        }
                        if (chipGroup_editTaskDialog_attachmentList != null) {
                            task.setAttachments(currentAttachments);
                        }
                        notifyItemChanged(adapterPosition);
                    });
                    builder.setNegativeButton(R.string.cancel, (dialog, id) -> {
                        // To-do: implement rollback
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    // Ustaw domyślną wartość pola taskName na obecną nazwę zadania
                    if (editText_editTaskDialog_taskName != null) {
                        editText_editTaskDialog_taskName.setText(task.getName());
                    }

                    // Ustaw przedział 1-10 dla selektora priorytetu i wyłącz zapętlenie wartości, pobierz obecną wartość
                    if (numberPicker_editTaskDialog_taskPriority != null) {
                        numberPicker_editTaskDialog_taskPriority.setMinValue(1);
                        numberPicker_editTaskDialog_taskPriority.setMaxValue(10);
                        numberPicker_editTaskDialog_taskPriority.setWrapSelectorWheel(false);
                        numberPicker_editTaskDialog_taskPriority.setValue(task.getPriority());
                    }

                    // Pobierz obecne ustawienie przypomnień
                    if (switch_editTaskDialog_setReminder != null) {
                        switch_editTaskDialog_setReminder.setChecked(task.isSetReminder());
                    }
                });

                /* Zmień status zadania po kliknięciu w checkbox */
                taskCheckbox.setOnClickListener(view -> {
                    adapterPosition = getAdapterPosition();
                    task = checkboxList.get(adapterPosition);
                    if (((AppCompatCheckBox) view).isChecked()) {
                        task.setStatus(context.getString(R.string.done));
                    } else {
                        task.setStatus(context.getString(R.string.not_done));
                    }
                    notifyItemChanged(adapterPosition); // Powiadom kontroler o kliknięciu
                });

                /* Usuń zadanie po kliknięciu w przycisk deleteTask */
                deleteTask.setOnClickListener(view -> {
                    // A właściwie to najpierw zapytaj o potwierdzenie...
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.niebezpieczna_operacja);
                    builder.setMessage(R.string.delete_task_confirmation_prompt);

                    builder.setPositiveButton(R.string.usun, (dialog, id) -> {
                        adapterPosition = getAdapterPosition();
                        task = checkboxList.get(adapterPosition);
                        checkboxList.remove(task);
                        if(checkboxList.size() == 0) {
                            // Powiadom fragment o usunięciu ostatniego zadania
                            lastTaskRemovedListener.onLastTaskRemoved();
                        }
                        notifyItemRemoved(adapterPosition);
                    });
                    builder.setNegativeButton(R.string.cancel, (dialog, id) -> {});

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    Button deleteBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    deleteBtn.setTextColor(Color.RED);
                });
            }
        }
    }

    private void buildTaskNotification(Task task) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.TaskMasterNotificationChannel))
                .setContentTitle(task.getName())
                .setContentText(context.getString(R.string.Masz_do_wykonania_zadanie) + task.getName() + context.getString(R.string.Kliknij_aby_zobaczyc_szczegoly))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setWhen(task.getDeadline().getTime())
                .setSmallIcon(R.drawable.priority_circular_background);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(task.getId(), builder.build());
    }

    protected void addAttachmentChip(ChipGroup targetChipGroup, Uri attachment) {
        currentAttachments.add(attachment);
        Chip attachmentChip = new Chip(context);

        attachmentChip.setHeight(ChipGroup.LayoutParams.MATCH_PARENT);
        attachmentChip.setWidth(ChipGroup.LayoutParams.WRAP_CONTENT);
        attachmentChip.setText(getFileNameFromUri(attachment));
        attachmentChip.setChipIcon(getFileIconFromUri(attachment));
        attachmentChip.setCloseIconVisible(true);

        attachmentChip.setOnCloseIconClickListener(view -> {
            targetChipGroup.removeView(view);
            currentAttachments.remove(attachment);
        });
        attachmentChip.setOnClickListener(view -> saveAttachment(context, attachment, attachmentChip.getText().toString()));

        targetChipGroup.addView(attachmentChip, 0);
    }

    private void saveAttachment(Context context, Uri uri, String fileName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            //noinspection IOStreamConstructor
            OutputStream outputStream = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/attachment_" + fileName);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            // Plik pobrany pomyślnie
            Toast.makeText(context, "Zapisano załącznik jako Downloads/attachment_" + fileName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            // Obtain permission to access the URI using ACTION_OPEN_DOCUMENT or related APIs
            try {
                context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex);
                    }
                }
            }
        } else if (uri.getScheme().equals("file")) {
            String lastPathSegment = uri.getLastPathSegment();
            if (lastPathSegment != null) {
                fileName = lastPathSegment;
            }
        }
        return fileName;
    }

    // Funkcja pomocnicza pobierająca generyczną ikonę dla danego typu pliku
    private Drawable getFileIconFromUri(Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        String mimeType = contentResolver.getType(uri);
        if (mimeType != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            PackageManager packageManager = context.getPackageManager();
            ResolveInfo info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null) {
                return info.loadIcon(packageManager);
            }
        }
        return ContextCompat.getDrawable(context, R.drawable.priority_circular_background);
    }

    // Funkcja pomocnicza aktualizująca wybrany termin ostateczny wykonania zadania
    private void updateDeadlineDateText(Calendar calendar, TextView textView) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM, yyyy", new Locale("pl"));
        textView.setText(dateFormat.format(calendar.getTime()));
    }
}
