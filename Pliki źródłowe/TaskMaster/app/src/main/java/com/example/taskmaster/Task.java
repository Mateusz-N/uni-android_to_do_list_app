package com.example.taskmaster;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Date;

@SuppressWarnings("unused") // Kompilator niepotrzebnie czepia się, że niektóre settery i gettery są nieużywane
public class Task {

    // <editor-fold defaultstate="collapsed" desc="Pola">
    private Context context;
    private static int taskCounter = 0;
    private int id;
    private String name;
    private int priority;
    private String status;
    private final Date added = new Date();
    private Date deadline = new Date();
    private boolean reminderIsSet;
    private ArrayList<Uri> attachments = new ArrayList<>();
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Gettery">
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public int getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public Date getAdded() {
        return added;
    }

    public Date getDeadline() {
        return deadline;
    }

    public boolean isSetReminder() {
        return reminderIsSet;
    }

    public ArrayList<Uri> getAttachments() {
        return attachments;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Settery">
    public void setContext(Context context) {
        this.context = context;
    }
    public void assignId(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.TaskMasterPrefs), Context.MODE_PRIVATE);
        int lastId = sharedPref.getInt("taskCount", -1);
        if(lastId == -1) {
            this.id = 0;
        }
        else {
            this.id = lastId;
        }
    }
    public void setPriority(int priority) {
        if(priority > 10 || priority < 1) {
            throw new IllegalArgumentException(context.getString(R.string.priorityOutOfRangeErr));
        }
        this.priority = priority;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public void setReminder(boolean setReminder) {
        this.reminderIsSet = setReminder;
    }

    public void setAttachments(ArrayList<Uri> attachments) {
        this.attachments = new ArrayList<>(attachments);
    }
    public void addAttachment(Uri attachment) {
        this.attachments.add(attachment);
    }
    public void removeAttachment(Uri attachment) {
        this.attachments.remove(attachment);
    }
    // </editor-fold>

    private static void reserveId(Context context, int id) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.TaskMasterPrefs), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
        sharedPrefEditor.putInt("taskId_" + id, id);
        sharedPrefEditor.putInt("taskCount" , id + 1);
        sharedPrefEditor.apply();
        taskCounter++;
    }

    public Task(Context context, String name, int priority, Date deadline, boolean setReminder) {
        this.setContext(context);
        this.assignId(context);
        this.setStatus(context.getString(R.string.not_done));
        this.setName(name);
        this.setPriority(priority);
        this.setDeadline(deadline);
        this.setReminder(setReminder);

        // Rezerwacja ID (dopisanie do SharedPreferences) na samym końcu, w razie gdyby w międzyczasie coś poszło nie tak
        reserveId(context, this.getId());
    }
    public Task(Context context, String name, Date deadline, int priority) {
        this.setContext(context);
        this.assignId(context);
        this.setStatus(context.getString(R.string.not_done));
        this.setName(name);
        this.setPriority(priority);
        this.setDeadline(deadline);
        this.setReminder(false);
        reserveId(context, this.getId());
    }

    public Task(Context context) {
        this.setContext(context);
        this.assignId(context);
        this.setStatus(context.getString(R.string.not_done));
        this.setName("Bez tytułu" + this.getId());
        this.setPriority(1);
        this.setReminder(false);
        reserveId(context, this.getId());
    }
}
