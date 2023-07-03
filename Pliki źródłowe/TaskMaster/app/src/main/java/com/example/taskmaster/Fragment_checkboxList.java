package com.example.taskmaster;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Objects;

public class Fragment_checkboxList extends Fragment implements CheckboxListAdapter.LastTaskRemovedListener {
    public interface LastTaskRemovedListener {
        void onLastTaskRemoved();
    }
    private LastTaskRemovedListener lastTaskRemovedListener;
    private ArrayList<Task> taskList = new ArrayList<>();
    @SuppressLint("NotifyDataSetChanged")
    protected void updateTaskList(ArrayList<Task> taskList) {
        this.taskList = taskList;
        if(this.checkboxListAdapter != null) {
            this.checkboxListAdapter.notifyDataSetChanged();
        }
    }
    protected ArrayList<Task> getTaskList() {
        return this.taskList;
    }
    private CheckboxListAdapter checkboxListAdapter;

    @Override
    public void onLastTaskRemoved() {
        if (lastTaskRemovedListener != null) {
            lastTaskRemovedListener.onLastTaskRemoved();
        }
    }
    public void setLastTaskRemovedListener(LastTaskRemovedListener listener) {
        this.lastTaskRemovedListener = listener;
    }

    private Context context = null;

    public Fragment_checkboxList() {
        // Required empty public constructor
    }

    public static Fragment_checkboxList newInstance() {
        return new Fragment_checkboxList();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            /* Pobierz referencje do aktywności wywołującej fragment */
            context = requireActivity();
        }
        catch (IllegalStateException e) {
            throw new IllegalStateException(String.valueOf(R.string.main_activity_must_implement_callbacks));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Nadmuchaj układ dla fragmentu (wygeneruj GUI)
        return inflater.inflate(R.layout.fragment_checkbox_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView checkboxList = requireView().findViewById(R.id.recyclerView_checkboxList);

        /* Dodanie do recyclerView linii rozdzielającej elementy */
        RecyclerView.ItemDecoration dividerItemDecoration = new DividerItemDecorator(ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright));
        checkboxList.addItemDecoration(dividerItemDecoration);

        /* Dodanie pierwszego zadania */
        /* Musi być tutaj, ponieważ wymaga istnienia widoku tworzonego w poprzedniej metodzie, onCreateView */
        try {
            /* Inicjalizacja taskList pierwszym zadaniem jeśli nie wczytano zadań z pliku */
            if(taskList.isEmpty()) {
                taskList.add(new Task(context));
            }

            /* Przygotowanie adaptera dla recyclerView, zainicjalizowanego danymi z taskList */
            checkboxList.setLayoutManager(new LinearLayoutManager(context));

            // Obsługa wyboru pliku podczas dodawania załącznika
            ActivityResultLauncher<String> attachmentContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        CheckboxListAdapter checkboxListAdapter = (CheckboxListAdapter) checkboxList.getAdapter();
//                        int adapterPosition = Objects.requireNonNull(checkboxListAdapter).getAdapterPosition();
//                        Task task = taskList.get(adapterPosition);
//                        task.addAttachment(uri);
//                        checkboxListAdapter.notifyItemChanged(adapterPosition); // model
                        Objects.requireNonNull(checkboxListAdapter).addAttachmentChip(checkboxListAdapter.getAttachmentChipGroup(), uri); // widok
                    }
                }
            );
            checkboxListAdapter = new CheckboxListAdapter(context, this.taskList, attachmentContentLauncher);
            checkboxListAdapter.setLastTaskRemovedListener(this);
            checkboxList.setAdapter(checkboxListAdapter);
        }
        catch(NullPointerException e) {
            Log.e(String.valueOf(R.string.fragment_checkboxlist_args_error), e.getMessage());
        }
    }
}