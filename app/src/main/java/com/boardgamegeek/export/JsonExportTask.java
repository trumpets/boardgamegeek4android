package com.boardgamegeek.export;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ExportProgressEvent;
import com.boardgamegeek.export.model.Model;
import com.boardgamegeek.extensions.AsyncTaskKt;
import com.boardgamegeek.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.Unit;
import timber.log.Timber;

public abstract class JsonExportTask<T extends Model> {
	@SuppressLint("StaticFieldLeak") @Nullable private final Context context;
	private final String type;
	private final Uri uri;
	private volatile boolean cancelled = false;

	public JsonExportTask(@Nullable Context context, String type, Uri uri) {
		this.context = context == null ? null : context.getApplicationContext();
		this.type = type;
		this.uri = uri;
	}

	protected int getVersion() {
		return 0;
	}

	protected abstract Cursor getCursor(Context context);

	protected abstract void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer);

	public void execute() {
		AsyncTaskKt.launchTaskWithResult(
			() -> doInBackground(),
			result -> {
				onPostExecute(result);
				return Unit.INSTANCE;
			}
		);
	}

	protected String doInBackground() {
		if (context == null) return "Error.";

		if (uri == null) {
			// Legacy path for old Android versions - should not be used on modern devices
			if (!FileUtils.isExtStorageAvailable()) {
				return context.getString(R.string.msg_export_failed_external_unavailable);
			}

			File exportPath = FileUtils.getExportPath();
			if (!exportPath.exists()) {
				if (!exportPath.mkdirs()) {
					return context.getString(R.string.msg_export_failed_external_not_created, exportPath);
				}
			}
		}

		if (isCancelled()) return context.getString(R.string.cancelled);

		OutputStream out;
		ParcelFileDescriptor pfd = null;
		if (uri == null) {
			File file = FileUtils.getExportFile(type);
			try {
				out = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				String error = context.getString(R.string.msg_export_failed_file_not_found, file);
				Timber.w(e, error);
				return error;
			}
		} else {
			try {
				pfd = context.getContentResolver().openFileDescriptor(uri, "w");
			} catch (SecurityException e) {
				String error = context.getString(R.string.msg_export_failed_permissions, uri);
				Timber.w(e, error);
				return error;
			} catch (FileNotFoundException e) {
				String error = context.getString(R.string.msg_export_failed_file_not_found, uri);
				Timber.w(e, error);
				return error;
			}
			if (pfd == null) {
				return context.getString(R.string.msg_export_failed_null_pfd, uri);
			}

			out = new FileOutputStream(pfd.getFileDescriptor());
		}

		final Cursor cursor = getCursor(context);
		if (cursor == null) return context.getString(R.string.msg_export_failed_null_cursor);

		try {
			writeJsonStream(out, cursor);
		} catch (Exception e) {
			String error = context.getString(R.string.msg_export_failed_write_json);
			Timber.e(e, error);
			return error;
		} finally {
			cursor.close();
		}

		FileUtils.closePfd(pfd);

		return null;
	}

	protected void publishProgress(int total, int current) {
		// Post progress on main thread using Handler
		new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
			EventBus.getDefault().post(new ExportProgressEvent(total, current, type));
		});
	}

	protected void onPostExecute(String errorMessage) {
		Timber.i(errorMessage);
		EventBus.getDefault().post(new ExportFinishedEvent(type, errorMessage));
	}

	public void cancel() {
		cancelled = true;
	}

	protected boolean isCancelled() {
		return cancelled;
	}

	private void writeJsonStream(@NonNull OutputStream out, @NonNull Cursor cursor) throws IOException {
		Gson gson = new GsonBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.create();
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
		writer.setIndent("  ");

		writer.beginObject();
		writer.name(Constants.NAME_TYPE).value(type);
		writer.name(Constants.NAME_VERSION).value(getVersion());
		writer.name(Constants.NAME_ITEMS);
		writer.beginArray();

		int numExported = 0;
		while (cursor.moveToNext()) {
			if (isCancelled()) break;
			publishProgress(cursor.getCount(), numExported++);
			try {
				writeJsonRecord(context, cursor, gson, writer);
			} catch (RuntimeException e) {
				Timber.e(e);
			}
		}

		writer.endArray();
		writer.endObject();
		writer.close();
	}
}
