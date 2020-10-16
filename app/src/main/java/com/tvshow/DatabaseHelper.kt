package com.tvshow

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        val query: String
        //creating table
        query = "CREATE TABLE " + TABLE_NAME + "(ID TEXT, JSON TEXT, Keyword TEXT)"
        sqLiteDatabase.execSQL(query)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        onCreate(sqLiteDatabase)
    }

    //add the new note
    fun addToDB(showData: ShowData, json: String?) {
        val sqLiteDatabase = this.writableDatabase
        val values = ContentValues()
        values.put("ID", showData.id)
        values.put("JSON", json)
        values.put("Keyword", showData.keyWord)
        //inserting new row
        sqLiteDatabase.insert(TABLE_NAME, null, values)
        //close database connection
        sqLiteDatabase.close()
    }


    fun getValueFromKeyword(keyWord: String): ShowData? {
        val query = "SELECT *FROM " + TABLE_NAME + " WHERE Keyword=" + "'" + keyWord + "'"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val showData = ShowData()
                val json = cursor.getString(cursor.getColumnIndex("JSON"))
                showData.parseJson(json)
                return showData
            }
        }
        cursor!!.close()
        return null
    }

    companion object {
        const val DATABASE_NAME = "SQLiteDatabase.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "show_list"
    }
}