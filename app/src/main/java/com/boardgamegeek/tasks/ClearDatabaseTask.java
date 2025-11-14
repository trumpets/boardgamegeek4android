package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;

import com.boardgamegeek.R;
import com.boardgamegeek.pref.SyncPrefs;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Avatars;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggContract.Thumbnails;

import timber.log.Timber;

/**
 * Deletes all data in the local database.
 */
public class ClearDatabaseTask extends ToastingAsyncTask {
	public ClearDatabaseTask(Context context) {
		super(context);
	}

	@Override
	protected int getSuccessMessageResource() {
		return R.string.pref_sync_clear_success;
	}

	@Override
	protected int getFailureMessageResource() {
		return R.string.pref_sync_clear_failure;
	}

	@Override
	protected Boolean doInBackground() {
		if (getContext() == null) return false;

		final ContentResolver resolver = getContext().getContentResolver();

		SyncPrefs.clearCollection(getContext());
		SyncPrefs.clearBuddyListTimestamps(getContext());
		SyncPrefs.clearPlaysTimestamps(getContext());

		int count = 0;
		count += delete(resolver, Games.CONTENT_URI);
		count += delete(resolver, Artists.CONTENT_URI);
		count += delete(resolver, Designers.CONTENT_URI);
		count += delete(resolver, Publishers.CONTENT_URI);
		count += delete(resolver, Categories.CONTENT_URI);
		count += delete(resolver, Mechanics.CONTENT_URI);
		count += delete(resolver, Buddies.CONTENT_URI);
		count += delete(resolver, Plays.CONTENT_URI);
		count += delete(resolver, CollectionViews.CONTENT_URI);
		Timber.i("Removed %d records", count);

		count = 0;
		count += resolver.delete(Thumbnails.CONTENT_URI, null, null);
		count += resolver.delete(Avatars.CONTENT_URI, null, null);
		Timber.i("Removed %d files", count);

		return true;
	}

	private int delete(ContentResolver resolver, Uri uri) {
		if (resolver == null) return 0;
		int count = resolver.delete(uri, null, null);
		Timber.i("Removed %1$d %2$s", count, uri.getLastPathSegment());
		return count;
	}
}
