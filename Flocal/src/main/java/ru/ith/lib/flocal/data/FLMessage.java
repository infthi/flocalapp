package ru.ith.lib.flocal.data;


import ru.ith.lib.flocal.FLDataLoader;

public class FLMessage {
	private final String userName;
	private final String text;
	private final String caption;
	private final String date;
	private final int rating;

    private final long mainID; //ID of thread-starting message. Required for responce.
	private final long ID;

	public FLMessage(String userName, String text, String caption, String date,
			int rating, long ID, long mainID) {
		this.userName = userName;
		this.text = text;
		this.caption = caption;
		this.date = date;
		this.rating = rating;
		this.ID = ID;
        this.mainID = mainID;
	}

	@Override
	public String toString() {
		return "{" + ID + "}" + userName + " @" + date + ": " + caption + "["
				+ rating + "]\n" + text;
	}

    public long getMainID() {
        return mainID;
    }

    public long getID() {
        return ID;
    }

    public String getPostData() {
        return text;
    }

    public String getAuthor() {
        return userName;
    }

    public String getDate() {
        return date;
    }

    public String getURL() {
        return "http://" + FLDataLoader.FLOCAL_HOST + "/showthreaded.php?Board=&Number=" + ID + "&src=";//TODO: parameters
    }
}
