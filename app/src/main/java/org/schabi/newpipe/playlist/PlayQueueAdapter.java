package org.schabi.newpipe.playlist;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.schabi.newpipe.R;
import org.schabi.newpipe.playlist.events.PlayQueueMessage;

import java.util.List;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

/**
 * Created by Christian Schabesberger on 01.08.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * InfoListAdapter.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class PlayQueueAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = PlayQueueAdapter.class.toString();

    private final PlayQueueItemBuilder playQueueItemBuilder;
    private final PlayQueue playQueue;
    private boolean showFooter = false;
    private View header = null;
    private View footer = null;

    private Disposable playQueueReactor;

    public class HFHolder extends RecyclerView.ViewHolder {
        public HFHolder(View v) {
            super(v);
            view = v;
        }
        public View view;
    }

    public void showFooter(final boolean show) {
        showFooter = show;
        notifyDataSetChanged();
    }

    public PlayQueueAdapter(final PlayQueue playQueue) {
        this.playQueueItemBuilder = new PlayQueueItemBuilder();
        this.playQueue = playQueue;

        startReactor();
    }

    public void setSelectedListener(final PlayQueueItemBuilder.OnSelectedListener listener) {
        playQueueItemBuilder.setOnSelectedListener(listener);
    }

    public void add(final List<PlayQueueItem> data) {
        playQueue.append(data);
    }

    public void add(final PlayQueueItem... data) {
        playQueue.append(data);
    }

    public void remove(final int index) {
        playQueue.remove(index);
    }

    private void startReactor() {
        final Observer<PlayQueueMessage> observer = new Observer<PlayQueueMessage>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                if (playQueueReactor != null) playQueueReactor.dispose();
                playQueueReactor = d;
            }

            @Override
            public void onNext(@NonNull PlayQueueMessage playQueueMessage) {
                notifyDataSetChanged();
            }

            @Override
            public void onError(@NonNull Throwable e) {}

            @Override
            public void onComplete() {
                dispose();
            }
        };

        playQueue.getBroadcastReceiver()
                .toObservable()
                .subscribe(observer);
    }

    public void dispose() {
        if (playQueueReactor != null) playQueueReactor.dispose();
        playQueueReactor = null;
    }

    public void setHeader(View header) {
        this.header = header;
        notifyDataSetChanged();
    }

    public void setFooter(View footer) {
        this.footer = footer;
        notifyDataSetChanged();
    }

    public List<PlayQueueItem> getItems() {
        return playQueue.getStreams();
    }

    @Override
    public int getItemCount() {
        int count = playQueue.getStreams().size();
        if(header != null) count++;
        if(footer != null && showFooter) count++;
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if(header != null && position == 0) {
            return 0;
        } else if(header != null) {
            position--;
        }
        if(footer != null && position == playQueue.getStreams().size() && showFooter) {
            return 1;
        }
        return 2;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        switch(type) {
            case 0:
                return new HFHolder(header);
            case 1:
                return new HFHolder(footer);
            case 2:
                return new PlayQueueItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.play_queue_item, parent, false));
            default:
                Log.e(TAG, "Trollolo");
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof PlayQueueItemHolder) {
            // Ensure header does not interfere with list building
            if (header != null) position--;
            // Build the list item
            playQueueItemBuilder.buildStreamInfoItem((PlayQueueItemHolder) holder, playQueue.getStreams().get(position));
            // Check if the current item should be selected/highlighted
            holder.itemView.setSelected(playQueue.getIndex() == position);
        } else if(holder instanceof HFHolder && position == 0 && header != null) {
            ((HFHolder) holder).view = header;
        } else if(holder instanceof HFHolder && position == playQueue.getStreams().size() && footer != null && showFooter) {
            ((HFHolder) holder).view = footer;
        }
    }
}
