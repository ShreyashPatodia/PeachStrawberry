package com.comp30022.helium.strawberry.entities;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Response;
import com.comp30022.helium.strawberry.components.server.PeachServerInterface;
import com.comp30022.helium.strawberry.components.server.exceptions.InstanceExpiredException;
import com.comp30022.helium.strawberry.components.server.rest.components.StrawberryListener;
import com.comp30022.helium.strawberry.entities.exceptions.FacebookIdNotSetException;
import com.comp30022.helium.strawberry.entities.cache.ImageCache;
import com.comp30022.helium.strawberry.entities.cache.UserCache;
import com.comp30022.helium.strawberry.patterns.exceptions.NotInstantiatedException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class User {
    private String id, username, facebookId;
//    private HashMap<ProfilePictureType, Bitmap> fbPictures = new HashMap<>();

    public static User getUser(String id) {
        UserCache userCache = UserCache.getInstance();

        if (userCache.get(id) != null)
            return userCache.get(id);

        User user = new User(id);
        userCache.put(id, user);
        return user;
    }

    public static User getUser(String id, String username) {
        UserCache userCache = UserCache.getInstance();

        if (userCache.get(id) != null)
            return userCache.get(id);

        User user = new User(id, username);
        userCache.put(id, user);
        return user;
    }

    public static User getUser(String id, String username, String facebookId) {
        UserCache userCache = UserCache.getInstance();

        if (userCache.get(id) != null)
            return userCache.get(id);

        User user = new User(id, username, facebookId);
        userCache.put(id, user);
        return user;
    }

    protected User(String id) {
        this.id = id;
        this.username = "";
        this.facebookId = "";
        updateUserInfo();
    }

    protected User(String id, String username) {
        this.id = id;
        this.username = username;
        this.facebookId = "";
        updateUserInfo();
    }

    protected User(String id, String username, String facebookId) {
        this.id = id;
        this.username = username;
        this.facebookId = facebookId;
    }

    protected void updateUserInfo() {
        Log.d("PeachUser", "update user info from init");
        UserCache userCache = UserCache.getInstance();

        User self = userCache.get(id);
        if (self != null) {
            if (self.getFacebookId().length() > 0) {
                this.facebookId = self.getFacebookId();
            }

            if (self.getUsername().length() > 0) {
                this.username = self.getUsername();
            }
        }

        if (facebookId.length() == 0 || username.length() == 0)
            try {
                PeachServerInterface.getInstance().getUser(id, new StrawberryListener(new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject self = new JSONObject(response.toString());
                            username = self.get("username").toString();
                            facebookId = self.get("facebookId").toString();
                            Log.d("PeachUser", "Username updated to " + username);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, null));

            } catch (NotInstantiatedException | InstanceExpiredException e) {
                e.printStackTrace();
            }
    }

    public void updateUserInfo(final StrawberryCallback<String> callback) {
        Log.d("PeachUser", "update user info from getfbid");
        try {
            PeachServerInterface.getInstance().getUser(id, new StrawberryListener(new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject self = new JSONObject(response.toString());
                        username = self.get("username").toString();
                        facebookId = self.get("facebookId").toString();
                        Log.d("PeachUser", "Username updated to " + username);

                        callback.run(username + " " + facebookId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, null));

        } catch (NotInstantiatedException | InstanceExpiredException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void getFacebookId(final StrawberryCallback<String> callback) {
        if (facebookId.length() == 0) {
            updateUserInfo(new StrawberryCallback<String>() {
                @Override
                public void run(String s) {
                    // now have it set
                    callback.run(facebookId);
                }
            });
        } else {
            callback.run(facebookId);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            User other = (User) obj;
            return other.getId().equals(this.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + username + " " + facebookId;
    }

    public static User toObject(String str) {
        String parts[] = str.split(" ");

        if (parts.length == 1) {
            return getUser(parts[0]);

        } else if (parts.length == 2) {
            return getUser(parts[0], parts[1]);

        } else if (parts.length == 3) {
            return getUser(parts[0], parts[1], parts[2]);
        }

        return null;
    }

    public void getFbPicture(final ProfilePictureType type, final StrawberryCallback<Bitmap> callback) throws FacebookIdNotSetException, MalformedURLException {
//        if (fbPictures.containsKey(type)) {
//            callback.run(fbPictures.get(type));
//            return;
//        }

        Bitmap cache = ImageCache.getInstance().get(id + type.toString());
        if (cache != null) {
            callback.run(cache);
            return;
        }

        if (facebookId.length() > 0) {
            URL imageURL = new URL("https://graph.facebook.com/" + facebookId + "/picture?type=" + type);

            AsyncTask<URL, Integer, Bitmap> task = new AsyncTask<URL, Integer, Bitmap>() {
                @Override
                protected Bitmap doInBackground(URL... urls) {
                    try {
                        Bitmap pic = BitmapFactory.decodeStream(urls[0].openConnection().getInputStream());
//                        fbPictures.put(type, pic);

                        ImageCache.getInstance().put(id + type.toString(), pic);
                        if (callback != null)
                            callback.run(pic);
                        return pic;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };

            task.execute(imageURL);
        }

        throw new FacebookIdNotSetException();
    }

    /**
     * Created by noxm on 4/10/17.
     */
    public static enum ProfilePictureType {
        SMALL("small"),
        NORMAL("normal"),
        LARGE("large"),
        SQUARE("square");

        String value;

        ProfilePictureType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
