package m.freezer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Base64;

public class SPHelper {
	private static final String NAME = "recent";
	private static final int VERSION = 2;
	private static SharedPreferences prefrence;
	
	private static final synchronized void ensureNotNull(Context context) {
		String fileName = NAME + "_" + VERSION;
		prefrence = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
	}
	
	public static final synchronized Object get(Context context, String key) {
		try {
			ensureNotNull(context);
			String base64 = prefrence.getString(key, "");
			if (TextUtils.isEmpty(base64)) {
				return null;
			}
			
			byte[] data = Base64.decode(base64, Base64.NO_WRAP);
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Object value = ois.readObject();
			ois.close();
			return value;
		} catch(Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
	
	public static final synchronized void put(Context context, String key, Object value) {
		if (value == null) {
			return;
		}
		
		try {
			ensureNotNull(context);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(value);
			oos.flush();
			oos.close();
			
			byte[] data = baos.toByteArray();
			String base64 = Base64.encodeToString(data, Base64.NO_WRAP);
			Editor editor = prefrence.edit();
			editor.putString(key, base64);
			editor.commit();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	public static final synchronized void remove(Context context, String key) {
		ensureNotNull(context);
		Editor editor = prefrence.edit();
		editor.remove(key);
		editor.commit();
	}
	
}
