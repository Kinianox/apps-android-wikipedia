package org.wikipedia.settings.languages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.language.LanguagesListActivity;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.MultiSelectActionModeCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static android.app.Activity.RESULT_OK;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE;

public class WikipediaLanguagesFragment extends Fragment implements WikipediaLanguagesItemView.Callback {
    @BindView(R.id.wikipedia_languages_recycler) RecyclerView recyclerView;
    private WikipediaApp app;
    private Unbinder unbinder;
    private ItemTouchHelper itemTouchHelper;
    private List<String> wikipediaLanguages = new ArrayList<>();
    private WikipediaLanguageItemAdapter adapter;
    private ActionMode actionMode;
    private MultiSelectCallback multiSelectCallback = new MultiSelectCallback();
    private List<String> selectedCodes = new ArrayList<>();
    private static final int NUM_HEADERS = 1;
    private static final int NUM_FOOTERS = 1;


    @NonNull public static WikipediaLanguagesFragment newInstance() {
        return new WikipediaLanguagesFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wikipedia_languages, container, false);
        app = WikipediaApp.getInstance();
        unbinder = ButterKnife.bind(this, view);

        prepareWikipediaLanguagesList();
        setupRecyclerView();

        // TODO: add funnel?

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_ADD_A_LANGUAGE
                && resultCode == RESULT_OK) {

            prepareWikipediaLanguagesList();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        recyclerView.setAdapter(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wikipedia_languages, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_wikipedia_languages_remove:
                beginRemoveLanguageMode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void prepareWikipediaLanguagesList() {
        wikipediaLanguages.clear();
        wikipediaLanguages.addAll(app.language().getAppLanguageCodes());
    }

    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        adapter = new WikipediaLanguageItemAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        itemTouchHelper = new ItemTouchHelper(new RearrangeableItemTouchHelperCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onCheckedChanged(int position) {
        toggleSelectedLanguage(wikipediaLanguages.get(position));
    }

    private void updateWikipediaLanguages() {
        app.language().setAppLanguageCodes(wikipediaLanguages);
        adapter.notifyDataSetChanged();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private final class WikipediaLanguageItemAdapter extends RecyclerView.Adapter<DefaultViewHolder> {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM = 1;
        private static final int VIEW_TYPE_FOOTER = 2;
        private boolean checkboxEnabled;

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return VIEW_TYPE_HEADER;
            } else if (position == getItemCount() - 1) {
                return VIEW_TYPE_FOOTER;
            } else {
                return VIEW_TYPE_ITEM;
            }
        }

        @Override
        public int getItemCount() {
            return wikipediaLanguages.size() + NUM_HEADERS + NUM_FOOTERS;
        }

        @Override
        public DefaultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            if (viewType == VIEW_TYPE_HEADER) {
                View view = inflater.inflate(R.layout.view_section_header, parent, false);
                return new HeaderViewHolder(view);
            } else if (viewType == VIEW_TYPE_FOOTER) {
                View view = inflater.inflate(R.layout.view_wikipedia_language_footer, parent, false);
                return new FooterViewHolder(view);
            } else {
                return new WikipediaLanguageItemHolder(new WikipediaLanguagesItemView(getContext()));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder holder, int pos) {
            if (holder instanceof WikipediaLanguageItemHolder) {
                ((WikipediaLanguageItemHolder) holder).bindItem(wikipediaLanguages.get(pos - NUM_HEADERS), pos - NUM_FOOTERS);
                ((WikipediaLanguageItemHolder) holder).getView().setCheckBoxEnabled(checkboxEnabled);
            }
        }

        @Override public void onViewAttachedToWindow(@NonNull DefaultViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof WikipediaLanguageItemHolder) {
                ((WikipediaLanguageItemHolder) holder).getView().setDragHandleTouchListener((v, event) -> {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            itemTouchHelper.startDrag(holder);
                            break;
                        case MotionEvent.ACTION_UP:
                            v.performClick();
                            break;
                        default:
                            break;
                    }
                    return false;
                });
                ((WikipediaLanguageItemHolder) holder).getView().setCallback(WikipediaLanguagesFragment.this);
            } else if (holder instanceof FooterViewHolder) {
                holder.getView().setOnClickListener(v -> {
                    startActivityForResult(new Intent(requireActivity(), LanguagesListActivity.class), ACTIVITY_REQUEST_ADD_A_LANGUAGE);
                    finishActionMode();
                });
            }
        }
        @Override public void onViewDetachedFromWindow(@NonNull DefaultViewHolder holder) {
            if (holder instanceof WikipediaLanguageItemHolder) {
                ((WikipediaLanguageItemHolder) holder).getView().setCallback(null);
                ((WikipediaLanguageItemHolder) holder).getView().setDragHandleTouchListener(null);
            }
            super.onViewDetachedFromWindow(holder);
        }

        void onMoveItem(int oldPosition, int newPosition) {
            Collections.swap(wikipediaLanguages, oldPosition - NUM_HEADERS, newPosition - NUM_FOOTERS);
            notifyItemMoved(oldPosition, newPosition);
        }

        void onCheckboxEnabled(boolean enabled) {
            checkboxEnabled = enabled;
        }
    }

    private final class RearrangeableItemTouchHelperCallback extends ItemTouchHelper.Callback {
        private final WikipediaLanguageItemAdapter adapter;

        RearrangeableItemTouchHelperCallback(WikipediaLanguageItemAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return viewHolder instanceof WikipediaLanguageItemHolder
                    ? makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) : -1;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            if (target instanceof WikipediaLanguageItemHolder) {
                adapter.onMoveItem(source.getAdapterPosition(), target.getAdapterPosition());
            }
            return true;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            updateWikipediaLanguages();
        }
    }

    // TODO: optimize and reuse the header view holder
    private class HeaderViewHolder extends DefaultViewHolder<View> {
        HeaderViewHolder(View itemView) {
            super(itemView);
            TextView sectionText = itemView.findViewById(R.id.section_header_text);
            sectionText.setText(R.string.wikipedia_languages_your_languages_text);
        }
    }

    private class WikipediaLanguageItemHolder extends DefaultViewHolder<WikipediaLanguagesItemView> {
        WikipediaLanguageItemHolder(WikipediaLanguagesItemView itemView) {
            super(itemView);
        }

        void bindItem(String languageCode, int position) {
            getView().setContents(app.language().getAppLanguageLocalizedName(languageCode), position);
        }
    }

    private class FooterViewHolder extends DefaultViewHolder<View> {
        FooterViewHolder(View itemView) {
            super(itemView);
        }
    }

    private void setMultiSelectEnabled(boolean enabled) {
        adapter.onCheckboxEnabled(enabled);
        adapter.notifyDataSetChanged();
    }

    private void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void beginRemoveLanguageMode() {
        ((AppCompatActivity) requireActivity()).startSupportActionMode(multiSelectCallback);
        setMultiSelectEnabled(true);
    }

    private void toggleSelectedLanguage(String code) {
        if (selectedCodes.contains(code)) {
            selectedCodes.remove(code);
        } else {
            selectedCodes.add(code);
        }
    }

    private void unselectAllLanguages() {
        selectedCodes.clear();
        adapter.notifyDataSetChanged();
    }

    private void deleteSelectedLanguages() {
        app.language().removeAppLanguageCodes(selectedCodes);
        prepareWikipediaLanguagesList();
        unselectAllLanguages();
    }

    private class MultiSelectCallback extends MultiSelectActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            super.onCreateActionMode(mode, menu);
            mode.setTitle(R.string.wikipedia_languages_remove_action_mode_title);
            mode.getMenuInflater().inflate(R.menu.menu_action_mode_wikipedia_languages, menu);
            actionMode = mode;
            selectedCodes.clear();
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onDeleteSelected() {
            showRemoveLanguagesDialog();
            // TODO: add snackbar for undo action?
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            unselectAllLanguages();
            setMultiSelectEnabled(false);
            actionMode = null;
            super.onDestroyActionMode(mode);
        }
    }

    public void showRemoveLanguagesDialog() {
        if (selectedCodes.size() > 0) {
            if (selectedCodes.size() < wikipediaLanguages.size()) {
                new AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.wikipedia_languages_remove_dialog_title)
                        .setMessage(R.string.wikipedia_languages_remove_dialog_content)
                        .setPositiveButton(android.R.string.ok, (dialog, i) -> {
                            deleteSelectedLanguages();
                            finishActionMode();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                new AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.wikipedia_languages_remove_warning_dialog_title)
                        .setMessage(R.string.wikipedia_languages_remove_warning_dialog_content)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }
    }
}
