package org.mobilburger.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import java.util.UUID;
/**
 * Created by hakimulin on 16.02.2017.
 */


public class DBHelper {

    public static enum Direction {
        WORD_TO_TRANSLATE,
        TRANSLATE_TO_WORD
    }

    public static final String DATABASE_NAME = "LearnWords.db";
    public static final String TB_WORDS = "words";
    public static final String TB_DICTS = "dictionaries";
    public static final String TB_STATS = "stats";
    public static final String TB_USERS = "users";

    public static final String CN_ID = "_id";
    public static final String CN_N = "n";
    public static final String CN_ID_DICT = "dict_id";
    public static final String CN_ID_WORD = "word_id";
    public static final String CN_TITLE = "title";
    public static final String CN_USER_NAME = "user_name";
    public static final String CN_WORD = "word";
    public static final String CN_TRANSLATE = "translate";
    public static final String CN_WORDS_PER_LESSON = "words_per_lesson";
    public static final String CN_WORDS_STUDY = "words_study";
    public static final String CN_USE_TIPS = "use_tips";
    public static final String CN_WRONG_ANSWERS_TO_SKIP = "wrong_answers_to_skip";
    public static final String CN_RIGHT_ANSWER_PERCENT = "right_answer_percent";
    public static final String CN_WRONG_ANSWER_PERCENT = "wrong_answer_percent";
    public static final String CN_EMPTY_ANSWER_IS_WRONG = "empty_answer_is_wrong";
    public static final String CN_DIRECTION = "direction";
    public static final String CN_COMPLETELY = "completely";
    public static final String CN_RIGHT = "right";
    public static final String CN_WRONG = "wrong";
    public static final String CN_DATE = "date";
    public static final String CN_LOCK = "lock";
    public static final String CN_EDITABLE = "editable";
    public static final String CN_PUBLIC = "public";


    public static String UUID() {
        return UUID.randomUUID().toString();
    }

    public static final String CREATE_TABLE_DICTS = "create table dictionaries (_id text primary key," +
            "title text not null, " +
            "user_name text default '', " +
            "editable int default 1)";
    public static final String CREATE_TABLE_WORDS = "create table words (_id text primary key, " +
            "dict_id text not null , " +
            "word text not null," +
            "translate text not null," +
            "lock int default 0," +
            "date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "unique (_id,dict_id))";
    public static final String CREATE_TABLE_STATS = "create table stats (_id text primary key, " +
            "dict_id text not null , " +
            "word_id text not null," +
            "direction text not null," +
            "completely int default 0)";

    public static final String CREATE_INDEX_TABLE_DICTS = "create index dicts_idx on dictionaries(title)";
    public static final String CREATE_INDEX_TABLE_WORDS = "create index words_idx on words(dict_id,word,translate)";
    public static final String CREATE_INDEX_TABLE_STATS = "create index stats_idx on stats(dict_id,word_id,direction)";

    private static SQLiteOpenHelper sqLiteOpenHelper;

    public static SQLiteOpenHelper getOpenHelper(Context context) {
        if (sqLiteOpenHelper == null) {
            sqLiteOpenHelper = new SQLiteOpenHelper(context, DATABASE_NAME, null, 5) {
                @Override
                public void onCreate(SQLiteDatabase db) {
                    db.execSQL(CREATE_TABLE_DICTS);
                    db.execSQL(CREATE_TABLE_WORDS);
                    db.execSQL(CREATE_INDEX_TABLE_DICTS);
                    db.execSQL(CREATE_INDEX_TABLE_WORDS);
                    db.execSQL(CREATE_TABLE_STATS);
                    db.execSQL(CREATE_INDEX_TABLE_STATS);
                }

                @Override
                public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                    if (oldVersion < 2) {
                        db.execSQL("alter table words add lock int default 0");
                    }
                    if (oldVersion < 3) {
                        db.execSQL("alter table dictionaries add editable int default 1");
                    }
                    if (oldVersion < 4) {
                        db.execSQL("alter table dictionaries add user_name text default ''");
                    }

                }
            };
        }
        return sqLiteOpenHelper;
    }

    public static String getWordsQuery(String dict_id, String direction, String condition) {

        String str =
                "select " +
                        "   w._id as _id," +
                        "   w.dict_id as dict_id," +
                        "   w.word as word," +
                        "   w.translate as translate," +
                        "   w.lock as lock," +
                        "   w.date as date," +
                        "   sum(ifnull(s.completely, 0)) as completely," +
                        "   sum(ifnull(s.right, 0)) as right," +
                        "   sum(ifnull(s.wrong, 0)) as wrong " +
                        "from " +
                        "   words as w " +
                        "       left join (select " +
                        "           stats.dict_id," +
                        "           stats.word_id," +
                        "           sum(case when stats.completely > 0 then 1 else 0 end) as right," +
                        "           sum(case when stats.completely < 0 then 1 else 0 end) as wrong," +
                        "           sum(stats.completely) as completely " +
                        "   from stats where " +
                        "           stats.dict_id = '[dict_id]' and stats.direction = '[direction]' " +
                        "   group by " +
                        "       stats.dict_id," +
                        "       stats.word_id) as s " +
                        "   on w._id = s.word_id " +
                        "   and w.dict_id = s.dict_id " +
                        "where w.dict_id = '[dict_id]' [condition] " +
                        "group by w._id,w.dict_id,w.word,w.translate,w.lock,w.date " +
                        "order by w.date ASC,completely DESC ";
        str = str.replace("[condition]", condition == null ? "" : "and " + condition);
        str = str.replace("[dict_id]", dict_id == null ? "" : dict_id);
        str = str.replace("[direction]", direction);
        return str;
    }
}
