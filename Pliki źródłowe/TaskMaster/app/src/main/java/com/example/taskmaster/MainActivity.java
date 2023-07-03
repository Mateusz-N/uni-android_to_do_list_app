package com.example.taskmaster;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

public final class MainActivity extends AppCompatActivity implements Fragment_checkboxList.LastTaskRemovedListener, PopupMenu.OnMenuItemClickListener {
    private Fragment_checkboxList fragment_checkboxList;
    private Context context;
    private ActivityResultLauncher<String> createDocumentLauncher;
    private ActivityResultLauncher<String> fileContentLauncher;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        createNotificationChannel();
        context = getApplicationContext();

        /* Obsługa zapisu zadań do pliku */
        createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument(),
                uri -> {
                    if(uri != null) {
                        try {
                            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                            if (outputStream != null) {
                                String taskList_asString = parseTasks(fragment_checkboxList.getTaskList());
                                outputStream.write(taskList_asString.getBytes());
                                outputStream.close();
                                Toast.makeText(context, context.getString(R.string.file_saved_successfully), Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            // Error occurred while writing the file
                        }
                    }
                }
        );
        /* Obsługa odczytu zadań z pliku */
        fileContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if(uri != null) {
                        fragment_checkboxList = Fragment_checkboxList.newInstance();
                        fragment_checkboxList.setLastTaskRemovedListener(this);
                        switchViews(R.id.rootLayout, fragment_checkboxList);
                        if (fragment_checkboxList != null) {
                            fragment_checkboxList.updateTaskList(getTasksFromFile(uri));
                        }
                    }
                }
        );

        /* Wyczyść ekran powitalny i dodaj pierwsze zadanie po naciśnięciu przycisku addFirstTask */
        FloatingActionButton fab_addFirstTask = findViewById(R.id.floatingActionButton_addTask);
        fab_addFirstTask.setOnClickListener(view -> {
            fragment_checkboxList = Fragment_checkboxList.newInstance();
            fragment_checkboxList.setLastTaskRemovedListener(this);
            switchViews(R.id.rootLayout, fragment_checkboxList);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_download) {
            if(fragment_checkboxList == null || fragment_checkboxList.getTaskList().isEmpty()) {
                Toast.makeText(context, context.getString(R.string.no_tasks_added_yet), Toast.LENGTH_SHORT).show();
                return false;
            }
            else {
                createDocumentLauncher.launch("text/plain");
                return true;
            }
        }
        else if(id == R.id.action_upload) {
            fileContentLauncher.launch("text/plain");
            return true;
        }
        else if(id == R.id.action_sort) {
            showPopupMenu(findViewById(R.id.action_sort));
            return true;
        }
        return false;
    }

    private void switchViews(int currentView, Fragment replacingView) {
        /* Prosta podmiana układu na fragment */
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(currentView, replacingView);
        ft.commit();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.TaskMasterNotificationChannel);
            String description = getString(R.string.Kanal_powiadomien_aplikacji_TaskMaster);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.TaskMasterNotificationChannel), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    @Override
    public void onLastTaskRemoved() {
        if(fragment_checkboxList != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment_checkboxList).commit();
            fragment_checkboxList = null;
        }
    }

    private ArrayList<Task> getTasksFromFile(Uri file) {
        return parseTaskFile(Objects.requireNonNull(readTaskFile(file)));
    }

    private String readTaskFile(Uri file) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            String fileContent = stringBuilder.toString();
            reader.close();
            return fileContent;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<Task> parseTaskFile(String fileContent) {
        ArrayList<Task> taskList = new ArrayList<>();
        ArrayList<String> taskList_asString = new ArrayList<>(Arrays.asList(fileContent.split("\n\n")));
        for(String task_asString : taskList_asString) {
            Task task = new Task(context);
            Log.e("task: ", task_asString);
            ArrayList<String> taskAttributes = new ArrayList<>(Arrays.asList(task_asString.split("\n")));
            ArrayList<String> taskAttachments_asString = new ArrayList<>(Arrays.asList(taskAttributes.get(taskAttributes.size() - 1).split("<attachment_separator>")));

            if(taskAttributes.size() == 6) {
                task.assignId(context);
                task.setName(taskAttributes.get(0));
                task.setPriority(Integer.parseInt(taskAttributes.get(1)));
                task.setStatus(taskAttributes.get(2));
                task.setDeadline(new Date(Long.parseLong(taskAttributes.get(3))));
                task.setReminder(Boolean.parseBoolean(taskAttributes.get(4)));
                for (String taskAttachment_asString : taskAttachments_asString) {
                    if(!taskAttachment_asString.equals("null")) {
                        task.addAttachment(Uri.parse(taskAttachment_asString));
                    }
                }
                taskList.add(task);
            }
        }
        return taskList;
    }

    private String parseTasks(ArrayList<Task> taskList) {
        StringBuilder tasks_asString = new StringBuilder();
        for(Task task : taskList) {
            StringBuilder taskAttachments_asString = new StringBuilder();
            ArrayList<Uri> taskAttachments = task.getAttachments();
            if(taskAttachments.size() > 0) {
                taskAttachments_asString.append(taskAttachments.get(0).toString());
                for(int attachmentNo = 1; attachmentNo < taskAttachments.size(); attachmentNo++) {
                    taskAttachments_asString
                            .append("<attachment_separator>")
                            .append(taskAttachments.get(attachmentNo).toString());
                }
            }
            else {
                taskAttachments_asString.append("null");
            }
            tasks_asString
                    .append(task.getName())
                    .append("\n")
                    .append(task.getPriority())
                    .append("\n")
                    .append(task.getStatus())
                    .append("\n")
                    .append(task.getDeadline().getTime())
                    .append("\n")
                    .append(task.isSetReminder())
                    .append("\n")
                    .append(taskAttachments_asString)
                    .append("\n\n");

        }
        return tasks_asString.toString();
    }
    private int selectedSortOption = -1;

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        selectedSortOption = id;
        item.setChecked(true);
        if(id == R.id.byName_desc) {
            return sortTasks(Task::getName, true);
        }
        else if(id == R.id.byName_asc) {
            return sortTasks(Task::getName, false);
        }
        else if(id == R.id.byPriority_desc) {
            return sortTasks(Task::getPriority, true);
        }
        else if(id == R.id.byPriority_asc) {
            return sortTasks(Task::getPriority, false);
        }
        else if(id == R.id.byStatusNotDoneFirst) {
            return sortTasks(Task::getStatus, true);
        }
        else if(id == R.id.byStatusDoneFirst) {
            return sortTasks(Task::getStatus, false);
        }
        return false;
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.setOnMenuItemClickListener(this);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.action_sort_dropdown, popupMenu.getMenu());
        if(selectedSortOption != -1) {
            popupMenu.getMenu().findItem(selectedSortOption).setChecked(true);
        }
        popupMenu.show();
    }

    private <U extends Comparable<? super U>> boolean sortTasks(Function<Task, U> by, boolean desc) {
        if(fragment_checkboxList == null || fragment_checkboxList.getTaskList().isEmpty()) {
            Toast.makeText(context, context.getString(R.string.no_tasks_added_yet), Toast.LENGTH_SHORT).show();
            return false;
        }
        else {
            ArrayList<Task> sortedTasks = fragment_checkboxList.getTaskList();
            if (desc) {
                sortedTasks.sort(Comparator.comparing(by).reversed());
            } else {
                sortedTasks.sort(Comparator.comparing(by));
            }
            fragment_checkboxList.updateTaskList(sortedTasks);
            return true;
        }
    }
}