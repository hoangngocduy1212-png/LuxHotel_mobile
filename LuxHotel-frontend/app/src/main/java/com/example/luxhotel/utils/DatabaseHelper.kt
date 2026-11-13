package com.example.luxhotel.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.luxhotel.models.*
import com.google.gson.annotations.SerializedName
import java.security.MessageDigest

/**
 * DatabaseHelper - quản lý toàn bộ SQLite cho LuxHotel.
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "luxhotel.db"
        private const val DATABASE_VERSION = 8

        // ===================== USERS =====================
        const val TABLE_USERS = "users"
        const val COL_USER_USERNAME = "username"
        const val COL_USER_PASSWORD = "password"
        const val COL_USER_ROLE = "role"
        const val COL_USER_DISPLAY_NAME = "display_name"
        const val COL_USER_PHONE = "phone"
        const val COL_USER_EMAIL = "email"
        const val COL_USER_DEPARTMENT = "department"
        const val COL_USER_JOIN_DATE = "join_date"
        const val COL_USER_STAFF_ID = "staff_id"
        const val COL_USER_IS_ACTIVE = "is_active"
        const val COL_USER_AVATAR = "avatar_path"

        // ===================== ROOMS =====================
        const val TABLE_ROOMS = "rooms"
        const val COL_ROOM_NUMBER = "room_number"
        const val COL_ROOM_TYPE = "room_type"
        const val COL_ROOM_TIER = "room_tier"
        const val COL_ROOM_PRICE = "price_per_night"
        const val COL_ROOM_STATUS = "status"
        const val COL_ROOM_FLOOR = "floor"
        const val COL_ROOM_DESCRIPTION = "description"
        const val COL_ROOM_UPDATED_AT = "updated_at"

        // ===================== ROOM DETAILS =====================
        const val TABLE_ROOM_DETAILS = "room_details"
        const val COL_DETAIL_ROOM_NUMBER = "room_number"
        const val COL_DETAIL_BED_COUNT = "bed_count"
        const val COL_DETAIL_AMENITIES = "amenities"

        // Room status values
        const val ROOM_STATUS_AVAILABLE   = "available"
        const val ROOM_STATUS_OCCUPIED    = "occupied"
        const val ROOM_STATUS_MAINTENANCE = "maintenance"
        const val ROOM_STATUS_BOOKED      = "booked"
        @Deprecated("Use ROOM_STATUS_MAINTENANCE instead", ReplaceWith("ROOM_STATUS_MAINTENANCE"))
        const val ROOM_STATUS_DIRTY       = "maintenance"

        // ===================== CUSTOMERS =====================
        const val TABLE_CUSTOMERS = "customers"
        const val COL_CUSTOMER_ID      = "customer_id"
        const val COL_CUSTOMER_NAME    = "full_name"
        const val COL_CUSTOMER_PHONE   = "phone"
        const val COL_CUSTOMER_ID_CARD = "id_card"
        const val COL_CUSTOMER_EMAIL   = "email"
        const val COL_CUSTOMER_ADDRESS = "address"

        // ===================== BOOKINGS =====================
        const val TABLE_BOOKINGS = "bookings"
        const val COL_BOOKING_ID              = "booking_id"
        const val COL_BOOKING_ROOM_NUMBER     = "room_number"
        const val COL_BOOKING_CUSTOMER_ID     = "customer_id"
        const val COL_BOOKING_CHECKIN_DATE    = "checkin_date"
        const val COL_BOOKING_CHECKOUT_DATE   = "checkout_date"
        const val COL_BOOKING_ACTUAL_CHECKIN  = "actual_checkin"
        const val COL_BOOKING_ACTUAL_CHECKOUT = "actual_checkout"
        const val COL_BOOKING_STATUS          = "status"
        const val COL_BOOKING_PRICE_PER_NIGHT = "price_per_night"
        const val COL_BOOKING_CREATED_BY      = "created_by"
        const val COL_BOOKING_CREATED_AT      = "created_at"

        const val BOOKING_STATUS_BOOKED      = "BOOKED"
        const val BOOKING_STATUS_CHECKED_IN  = "CHECKED_IN"
        const val BOOKING_STATUS_CHECKED_OUT = "CHECKED_OUT"
        const val BOOKING_STATUS_CANCELLED   = "CANCELLED"

        // ===================== SERVICE CHARGES =====================
        const val TABLE_SERVICE_CHARGES  = "service_charges"
        const val COL_SERVICE_ID         = "service_id"
        const val COL_SERVICE_BOOKING_ID = "booking_id"
        const val COL_SERVICE_NAME       = "name"
        const val COL_SERVICE_AMOUNT     = "amount"

        // ===================== HOUSEKEEPING TASKS =====================
        const val TABLE_HOUSEKEEPING_TASKS = "housekeeping_tasks"
        const val COL_TASK_ID          = "task_id"
        const val COL_TASK_ROOM_NUMBER = "room_number"
        const val COL_TASK_TYPE        = "task_type"
        const val COL_TASK_LABEL       = "label"
        const val COL_TASK_PRIORITY    = "priority"
        const val COL_TASK_IS_DONE     = "is_done"
        const val COL_TASK_ASSIGNED_TO = "assigned_to"
        const val COL_TASK_DATE        = "task_date"

        // ===================== MAINTENANCE REPORTS =====================
        const val TABLE_MAINTENANCE_REPORTS = "maintenance_reports"
        const val COL_REPORT_ID          = "report_id"
        const val COL_REPORT_ROOM_NUMBER = "room_number"
        const val COL_REPORT_DESCRIPTION = "description"
        const val COL_REPORT_REPORTED_BY = "reported_by"
        const val COL_REPORT_STATUS      = "status"
        const val COL_REPORT_CREATED_AT  = "created_at"
        const val COL_REPORT_IMAGE_PATH  = "image_path"

        const val REPORT_STATUS_PENDING     = "PENDING"
        const val REPORT_STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val REPORT_STATUS_DONE        = "DONE"

        // ===================== SYNC QUEUE =====================
        const val TABLE_SYNC_QUEUE = "sync_queue"
        const val COL_SYNC_ID = "id"
        const val COL_SYNC_ACTION_TYPE = "action_type"   // CHECKIN | CHECKOUT | ROOM_STATUS | TASK_DONE | MAINTENANCE_STATUS
        const val COL_SYNC_TARGET_ID = "target_id"       // booking_id / room_number / task_id / report_id
        const val COL_SYNC_PAYLOAD = "payload"           // JSON string chứa body cần gửi (nếu có), vd {"status":"maintenance"}
        const val COL_SYNC_CREATED_AT = "created_at"
        const val COL_SYNC_STATUS = "sync_status"        // PENDING | FAILED
        const val COL_SYNC_RETRY_COUNT = "retry_count"

        const val SYNC_STATUS_PENDING = "PENDING"
        const val SYNC_STATUS_FAILED = "FAILED"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_USERS ($COL_USER_USERNAME TEXT PRIMARY KEY, $COL_USER_PASSWORD TEXT NOT NULL, $COL_USER_ROLE TEXT NOT NULL, $COL_USER_DISPLAY_NAME TEXT NOT NULL, $COL_USER_PHONE TEXT, $COL_USER_EMAIL TEXT, $COL_USER_DEPARTMENT TEXT, $COL_USER_JOIN_DATE TEXT, $COL_USER_STAFF_ID TEXT, $COL_USER_IS_ACTIVE INTEGER NOT NULL DEFAULT 1, $COL_USER_AVATAR TEXT)")
        db.execSQL("CREATE TABLE $TABLE_ROOMS ($COL_ROOM_NUMBER TEXT PRIMARY KEY, $COL_ROOM_TYPE TEXT NOT NULL, $COL_ROOM_TIER TEXT NOT NULL DEFAULT 'STANDARD', $COL_ROOM_PRICE INTEGER NOT NULL DEFAULT 0, $COL_ROOM_STATUS TEXT NOT NULL DEFAULT '$ROOM_STATUS_AVAILABLE', $COL_ROOM_FLOOR INTEGER DEFAULT 1, $COL_ROOM_DESCRIPTION TEXT, $COL_ROOM_UPDATED_AT TEXT)")
        db.execSQL("CREATE TABLE $TABLE_ROOM_DETAILS ($COL_DETAIL_ROOM_NUMBER TEXT PRIMARY KEY, $COL_DETAIL_BED_COUNT INTEGER NOT NULL DEFAULT 1, $COL_DETAIL_AMENITIES TEXT, FOREIGN KEY($COL_DETAIL_ROOM_NUMBER) REFERENCES $TABLE_ROOMS($COL_ROOM_NUMBER))")
        db.execSQL("CREATE TABLE $TABLE_CUSTOMERS ($COL_CUSTOMER_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_CUSTOMER_NAME TEXT NOT NULL, $COL_CUSTOMER_PHONE TEXT, $COL_CUSTOMER_ID_CARD TEXT, $COL_CUSTOMER_EMAIL TEXT, $COL_CUSTOMER_ADDRESS TEXT)")
        db.execSQL("CREATE TABLE $TABLE_BOOKINGS ($COL_BOOKING_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_BOOKING_ROOM_NUMBER TEXT NOT NULL, $COL_BOOKING_CUSTOMER_ID INTEGER NOT NULL, $COL_BOOKING_CHECKIN_DATE TEXT NOT NULL, $COL_BOOKING_CHECKOUT_DATE TEXT NOT NULL, $COL_BOOKING_ACTUAL_CHECKIN TEXT, $COL_BOOKING_ACTUAL_CHECKOUT TEXT, $COL_BOOKING_STATUS TEXT NOT NULL DEFAULT '$BOOKING_STATUS_BOOKED', $COL_BOOKING_PRICE_PER_NIGHT INTEGER NOT NULL DEFAULT 0, $COL_BOOKING_CREATED_BY TEXT, $COL_BOOKING_CREATED_AT TEXT, FOREIGN KEY($COL_BOOKING_ROOM_NUMBER) REFERENCES $TABLE_ROOMS($COL_ROOM_NUMBER), FOREIGN KEY($COL_BOOKING_CUSTOMER_ID) REFERENCES $TABLE_CUSTOMERS($COL_CUSTOMER_ID))")
        db.execSQL("CREATE TABLE $TABLE_SERVICE_CHARGES ($COL_SERVICE_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_SERVICE_BOOKING_ID INTEGER NOT NULL, $COL_SERVICE_NAME TEXT NOT NULL, $COL_SERVICE_AMOUNT INTEGER NOT NULL DEFAULT 0, FOREIGN KEY($COL_SERVICE_BOOKING_ID) REFERENCES $TABLE_BOOKINGS($COL_BOOKING_ID))")
        db.execSQL("CREATE TABLE $TABLE_HOUSEKEEPING_TASKS ($COL_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_TASK_ROOM_NUMBER TEXT NOT NULL, $COL_TASK_TYPE TEXT NOT NULL, $COL_TASK_LABEL TEXT, $COL_TASK_PRIORITY TEXT NOT NULL DEFAULT 'NORMAL', $COL_TASK_IS_DONE INTEGER NOT NULL DEFAULT 0, $COL_TASK_ASSIGNED_TO TEXT, $COL_TASK_DATE TEXT NOT NULL, FOREIGN KEY($COL_TASK_ROOM_NUMBER) REFERENCES $TABLE_ROOMS($COL_ROOM_NUMBER), FOREIGN KEY($COL_TASK_ASSIGNED_TO) REFERENCES $TABLE_USERS($COL_USER_USERNAME))")
        db.execSQL("CREATE TABLE $TABLE_MAINTENANCE_REPORTS ($COL_REPORT_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_REPORT_ROOM_NUMBER TEXT NOT NULL, $COL_REPORT_DESCRIPTION TEXT NOT NULL, $COL_REPORT_REPORTED_BY TEXT, $COL_REPORT_STATUS TEXT NOT NULL DEFAULT '$REPORT_STATUS_PENDING', $COL_REPORT_CREATED_AT TEXT, $COL_REPORT_IMAGE_PATH TEXT, FOREIGN KEY($COL_REPORT_ROOM_NUMBER) REFERENCES $TABLE_ROOMS($COL_ROOM_NUMBER), FOREIGN KEY($COL_REPORT_REPORTED_BY) REFERENCES $TABLE_USERS($COL_USER_USERNAME))")
        db.execSQL("CREATE TABLE $TABLE_SYNC_QUEUE ($COL_SYNC_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_SYNC_ACTION_TYPE TEXT NOT NULL, $COL_SYNC_TARGET_ID TEXT NOT NULL, $COL_SYNC_PAYLOAD TEXT, $COL_SYNC_CREATED_AT TEXT, $COL_SYNC_STATUS TEXT DEFAULT '$SYNC_STATUS_PENDING', $COL_SYNC_RETRY_COUNT INTEGER DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_USER_IS_ACTIVE INTEGER NOT NULL DEFAULT 1")
            }
            if (oldVersion < 7) {
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_USER_AVATAR TEXT")
            }
            if (oldVersion < 6) {
                db.execSQL("ALTER TABLE $TABLE_MAINTENANCE_REPORTS ADD COLUMN $COL_REPORT_IMAGE_PATH TEXT")
            }
            if (oldVersion < 8) {
                db.execSQL("ALTER TABLE $TABLE_ROOMS ADD COLUMN $COL_ROOM_UPDATED_AT TEXT")
                db.execSQL("UPDATE $TABLE_ROOMS SET $COL_ROOM_UPDATED_AT = '${currentDateString()}'")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun login(username: String, password: String): User? {
        val db = readableDatabase
        val hashed = hashPassword(password)
        val cols = arrayOf(COL_USER_USERNAME, COL_USER_ROLE, COL_USER_DISPLAY_NAME, COL_USER_IS_ACTIVE, COL_USER_PHONE, COL_USER_EMAIL, COL_USER_DEPARTMENT, COL_USER_JOIN_DATE, COL_USER_STAFF_ID, COL_USER_AVATAR)
        val cursor = db.query(TABLE_USERS, cols, "$COL_USER_USERNAME = ? AND $COL_USER_PASSWORD = ?", arrayOf(username, hashed), null, null, null)
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                username = cursor.getString(0),
                role = UserRole.valueOf(cursor.getString(1)),
                displayName = cursor.getString(2),
                isActive = cursor.getInt(3) == 1,
                phone = cursor.getString(4) ?: "",
                email = cursor.getString(5) ?: "",
                department = cursor.getString(6) ?: "",
                joinDate = cursor.getString(7) ?: "",
                staffId = cursor.getString(8) ?: "",
                avatarPath = cursor.getString(9)
            )
        }
        cursor.close()
        return user
    }

    fun getUser(username: String): User? {
        val db = readableDatabase
        val cols = arrayOf(COL_USER_USERNAME, COL_USER_ROLE, COL_USER_DISPLAY_NAME, COL_USER_IS_ACTIVE, COL_USER_PHONE, COL_USER_EMAIL, COL_USER_DEPARTMENT, COL_USER_JOIN_DATE, COL_USER_STAFF_ID, COL_USER_AVATAR)
        val cursor = db.query(TABLE_USERS, cols, "$COL_USER_USERNAME = ?", arrayOf(username), null, null, null)
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                username = cursor.getString(0),
                role = UserRole.valueOf(cursor.getString(1)),
                displayName = cursor.getString(2),
                isActive = cursor.getInt(3) == 1,
                phone = cursor.getString(4) ?: "",
                email = cursor.getString(5) ?: "",
                department = cursor.getString(6) ?: "",
                joinDate = cursor.getString(7) ?: "",
                staffId = cursor.getString(8) ?: "",
                avatarPath = cursor.getString(9)
            )
        }
        cursor.close()
        return user
    }

    fun getAllUsers(role: UserRole? = null): List<User> {
        val db = readableDatabase
        val list = mutableListOf<User>()
        val cols = arrayOf(COL_USER_USERNAME, COL_USER_ROLE, COL_USER_DISPLAY_NAME, COL_USER_IS_ACTIVE, COL_USER_PHONE, COL_USER_EMAIL, COL_USER_DEPARTMENT, COL_USER_JOIN_DATE, COL_USER_STAFF_ID, COL_USER_AVATAR)
        val cursor = if (role != null) {
            db.query(TABLE_USERS, cols, "$COL_USER_ROLE = ?", arrayOf(role.name), null, null, null)
        } else {
            db.query(TABLE_USERS, cols, null, null, null, null, null)
        }
        while (cursor.moveToNext()) {
            list.add(User(
                username = cursor.getString(0),
                role = UserRole.valueOf(cursor.getString(1)),
                displayName = cursor.getString(2),
                isActive = cursor.getInt(3) == 1,
                phone = cursor.getString(4) ?: "",
                email = cursor.getString(5) ?: "",
                department = cursor.getString(6) ?: "",
                joinDate = cursor.getString(7) ?: "",
                staffId = cursor.getString(8) ?: "",
                avatarPath = cursor.getString(9)
            ))
        }
        cursor.close()
        return list
    }

    fun changePassword(username: String, newPassword: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply { put(COL_USER_PASSWORD, hashPassword(newPassword)) }
        return db.update(TABLE_USERS, cv, "$COL_USER_USERNAME = ?", arrayOf(username)) > 0
    }

    fun updateUserProfile(username: String, displayName: String, phone: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply { put(COL_USER_DISPLAY_NAME, displayName); put(COL_USER_PHONE, phone) }
        return db.update(TABLE_USERS, cv, "$COL_USER_USERNAME = ?", arrayOf(username)) > 0
    }

    fun addUser(username: String, password: String, role: UserRole, displayName: String, phone: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_USER_USERNAME, username)
            put(COL_USER_PASSWORD, hashPassword(password))
            put(COL_USER_ROLE, role.name)
            put(COL_USER_DISPLAY_NAME, displayName)
            put(COL_USER_PHONE, phone)
            put(COL_USER_IS_ACTIVE, 1)
        }
        return db.insert(TABLE_USERS, null, cv) != -1L
    }

    fun setUserActive(username: String, isActive: Boolean): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply { put(COL_USER_IS_ACTIVE, if (isActive) 1 else 0) }
        return db.update(TABLE_USERS, cv, "$COL_USER_USERNAME = ?", arrayOf(username)) > 0
    }

    fun updateUserAvatar(username: String, avatarPath: String?): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply { put(COL_USER_AVATAR, avatarPath) }
        return db.update(TABLE_USERS, cv, "$COL_USER_USERNAME = ?", arrayOf(username)) > 0
    }

    // =====================================================================
    // ROOMS
    // =====================================================================

    fun addRoom(roomNumber: String, roomType: String, tier: RoomTier, price: Long, floor: Int, bedCount: Int = 1, description: String? = null): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply { put(COL_ROOM_NUMBER, roomNumber); put(COL_ROOM_TYPE, roomType); put(COL_ROOM_TIER, tier.name); put(COL_ROOM_PRICE, price); put(COL_ROOM_STATUS, ROOM_STATUS_AVAILABLE); put(COL_ROOM_FLOOR, floor); put(COL_ROOM_DESCRIPTION, description) }
            val res1 = db.insert(TABLE_ROOMS, null, cv)
            val detailCv = ContentValues().apply { put(COL_DETAIL_ROOM_NUMBER, roomNumber); put(COL_DETAIL_BED_COUNT, bedCount) }
            val res2 = db.insert(TABLE_ROOM_DETAILS, null, detailCv)
            if (res1 != -1L && res2 != -1L) { db.setTransactionSuccessful(); return true }
            return false
        } finally { db.endTransaction() }
    }

    fun getAllRoomsInfo(): List<RoomInfo> {
        val db = readableDatabase
        val list = mutableListOf<RoomInfo>()
        val cursor = db.rawQuery(
            """
            SELECT r.$COL_ROOM_NUMBER, r.$COL_ROOM_TYPE, r.$COL_ROOM_STATUS,
                   r.$COL_ROOM_PRICE, d.$COL_DETAIL_BED_COUNT, c.$COL_CUSTOMER_NAME
            FROM $TABLE_ROOMS r
            LEFT JOIN $TABLE_ROOM_DETAILS d ON r.$COL_ROOM_NUMBER = d.$COL_DETAIL_ROOM_NUMBER
            LEFT JOIN $TABLE_BOOKINGS b ON b.$COL_BOOKING_ROOM_NUMBER = r.$COL_ROOM_NUMBER AND b.$COL_BOOKING_STATUS = 'CHECKED_IN'
            LEFT JOIN $TABLE_CUSTOMERS c ON c.$COL_CUSTOMER_ID = b.$COL_BOOKING_CUSTOMER_ID
            ORDER BY CAST(r.$COL_ROOM_NUMBER AS INTEGER) ASC
            """.trimIndent(), null
        )
        while (cursor.moveToNext()) {
            list.add(RoomInfo(roomNumber = cursor.getString(0), roomType = cursor.getString(1), status = cursor.getString(2), price = cursor.getLong(3), bedCount = cursor.getInt(4), guestName = if (cursor.isNull(5)) null else cursor.getString(5)))
        }
        cursor.close()
        return list
    }

    fun getAvailableRooms(): List<RoomInfo> {
        val db = readableDatabase
        val list = mutableListOf<RoomInfo>()
        val cursor = db.rawQuery(
            """
            SELECT r.$COL_ROOM_NUMBER, r.$COL_ROOM_TYPE, r.$COL_ROOM_STATUS, r.$COL_ROOM_PRICE, d.$COL_DETAIL_BED_COUNT
            FROM $TABLE_ROOMS r
            LEFT JOIN $TABLE_ROOM_DETAILS d ON r.$COL_ROOM_NUMBER = d.$COL_DETAIL_ROOM_NUMBER
            WHERE r.$COL_ROOM_STATUS = '$ROOM_STATUS_AVAILABLE'
            ORDER BY CAST(r.$COL_ROOM_NUMBER AS INTEGER) ASC
            """.trimIndent(), null
        )
        while (cursor.moveToNext()) {
            list.add(RoomInfo(roomNumber = cursor.getString(0), roomType = cursor.getString(1), status = cursor.getString(2), price = cursor.getLong(3), bedCount = cursor.getInt(4), guestName = null))
        }
        cursor.close()
        return list
    }

    fun updateRoomStatus(roomNumber: String, status: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply { 
            put(COL_ROOM_STATUS, status)
            put(COL_ROOM_UPDATED_AT, currentDateString())
        }
        return db.update(TABLE_ROOMS, cv, "$COL_ROOM_NUMBER = ?", arrayOf(roomNumber)) > 0
    }

    fun syncRooms(rooms: List<RoomInfo>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (room in rooms) {
                val cv = ContentValues().apply {
                    put(COL_ROOM_NUMBER, room.roomNumber)
                    put(COL_ROOM_TYPE, room.roomType)
                    put(COL_ROOM_TIER, room.roomTier ?: "STANDARD")
                    put(COL_ROOM_STATUS, room.status)
                    put(COL_ROOM_PRICE, room.price)
                    put(COL_ROOM_FLOOR, room.floor ?: 1)
                    put(COL_ROOM_DESCRIPTION, room.description)
                    put(COL_ROOM_UPDATED_AT, currentDateString())
                }
                val rows = db.update(TABLE_ROOMS, cv, "$COL_ROOM_NUMBER = ?", arrayOf(room.roomNumber))
                if (rows == 0) {
                    db.insert(TABLE_ROOMS, null, cv)
                }

                val detailCv = ContentValues().apply {
                    put(COL_DETAIL_ROOM_NUMBER, room.roomNumber)
                    put(COL_DETAIL_BED_COUNT, room.bedCount)
                }
                val detailRows = db.update(TABLE_ROOM_DETAILS, detailCv, "$COL_DETAIL_ROOM_NUMBER = ?", arrayOf(room.roomNumber))
                if (detailRows == 0) {
                    db.insert(TABLE_ROOM_DETAILS, null, detailCv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getRoomPrice(roomNumber: String): Long {
        val db = readableDatabase
        val cursor = db.query(TABLE_ROOMS, arrayOf(COL_ROOM_PRICE), "$COL_ROOM_NUMBER = ?", arrayOf(roomNumber), null, null, null)
        var price = 0L
        if (cursor.moveToFirst()) price = cursor.getLong(0)
        cursor.close()
        return price
    }

    fun getRoomStatusCounts(): Map<String, Int> {
        val result = mutableMapOf(
            ROOM_STATUS_AVAILABLE to 0,
            ROOM_STATUS_OCCUPIED to 0,
            ROOM_STATUS_MAINTENANCE to 0,
            ROOM_STATUS_BOOKED to 0
        )
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT $COL_ROOM_STATUS, COUNT(*) FROM $TABLE_ROOMS GROUP BY $COL_ROOM_STATUS", null)
            cursor?.use {
                while (it.moveToNext()) {
                    val status = it.getString(0)
                    if (status != null) {
                        result[status] = it.getInt(1)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    // =====================================================================
    // CUSTOMERS
    // =====================================================================

    fun addCustomer(name: String, phone: String, idCard: String? = null, email: String? = null, address: String? = null): Long {
        val db = writableDatabase
        val cv = ContentValues().apply { put(COL_CUSTOMER_NAME, name); put(COL_CUSTOMER_PHONE, phone); put(COL_CUSTOMER_ID_CARD, idCard); put(COL_CUSTOMER_EMAIL, email); put(COL_CUSTOMER_ADDRESS, address) }
        return db.insert(TABLE_CUSTOMERS, null, cv)
    }

    fun findCustomerByPhone(phone: String): Long? {
        val db = readableDatabase
        val cursor = db.query(TABLE_CUSTOMERS, arrayOf(COL_CUSTOMER_ID), "$COL_CUSTOMER_PHONE = ?", arrayOf(phone), null, null, null)
        var id: Long? = null
        if (cursor.moveToFirst()) id = cursor.getLong(0)
        cursor.close()
        return id
    }

    fun getAllCustomersWithStatus(): List<Customer> {
        val db = readableDatabase
        val list = mutableListOf<Customer>()
        var cursor = db.rawQuery("SELECT c.$COL_CUSTOMER_ID, c.$COL_CUSTOMER_NAME, b.$COL_BOOKING_ROOM_NUMBER, r.$COL_ROOM_TYPE, c.$COL_CUSTOMER_PHONE FROM $TABLE_BOOKINGS b JOIN $TABLE_CUSTOMERS c ON c.$COL_CUSTOMER_ID = b.$COL_BOOKING_CUSTOMER_ID JOIN $TABLE_ROOMS r ON r.$COL_ROOM_NUMBER = b.$COL_BOOKING_ROOM_NUMBER WHERE b.$COL_BOOKING_STATUS = 'CHECKED_IN'", null)
        while (cursor.moveToNext()) { list.add(Customer(id = cursor.getLong(0).toString(), name = cursor.getString(1), roomNumber = cursor.getString(2), roomType = cursor.getString(3), status = CustomerStatus.STAYING, phone = cursor.getString(4) ?: "")) }
        cursor.close()
        cursor = db.rawQuery("SELECT c.$COL_CUSTOMER_ID, c.$COL_CUSTOMER_NAME, b.$COL_BOOKING_ROOM_NUMBER, r.$COL_ROOM_TYPE, c.$COL_CUSTOMER_PHONE FROM $TABLE_BOOKINGS b JOIN $TABLE_CUSTOMERS c ON c.$COL_CUSTOMER_ID = b.$COL_BOOKING_CUSTOMER_ID JOIN $TABLE_ROOMS r ON r.$COL_ROOM_NUMBER = b.$COL_BOOKING_ROOM_NUMBER WHERE b.$COL_BOOKING_STATUS = 'BOOKED'", null)
        while (cursor.moveToNext()) { list.add(Customer(id = cursor.getLong(0).toString(), name = cursor.getString(1), roomNumber = cursor.getString(2), roomType = cursor.getString(3), status = CustomerStatus.ARRIVING, phone = cursor.getString(4) ?: "")) }
        cursor.close()
        cursor = db.rawQuery("SELECT c.$COL_CUSTOMER_ID, c.$COL_CUSTOMER_NAME, b.$COL_BOOKING_ROOM_NUMBER, r.$COL_ROOM_TYPE, c.$COL_CUSTOMER_PHONE FROM $TABLE_BOOKINGS b JOIN $TABLE_CUSTOMERS c ON c.$COL_CUSTOMER_ID = b.$COL_BOOKING_CUSTOMER_ID JOIN $TABLE_ROOMS r ON r.$COL_ROOM_NUMBER = b.$COL_BOOKING_ROOM_NUMBER WHERE b.$COL_BOOKING_STATUS = 'CHECKED_OUT' AND b.$COL_BOOKING_ID = (SELECT MAX(b2.$COL_BOOKING_ID) FROM $TABLE_BOOKINGS b2 WHERE b2.$COL_BOOKING_CUSTOMER_ID = c.$COL_CUSTOMER_ID)", null)
        while (cursor.moveToNext()) { list.add(Customer(id = cursor.getLong(0).toString(), name = cursor.getString(1), roomNumber = cursor.getString(2), roomType = cursor.getString(3), status = CustomerStatus.LEFT, phone = cursor.getString(4) ?: "")) }
        cursor.close()
        return list
    }

    // =====================================================================
    // BOOKINGS
    // =====================================================================

    fun createBooking(roomNumber: String, customerId: Long, checkinDate: String, checkoutDate: String, pricePerNight: Long, createdBy: String, checkInNow: Boolean = false): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply { put(COL_BOOKING_ROOM_NUMBER, roomNumber); put(COL_BOOKING_CUSTOMER_ID, customerId); put(COL_BOOKING_CHECKIN_DATE, checkinDate); put(COL_BOOKING_CHECKOUT_DATE, checkoutDate); put(COL_BOOKING_PRICE_PER_NIGHT, pricePerNight); put(COL_BOOKING_CREATED_BY, createdBy); put(COL_BOOKING_CREATED_AT, System.currentTimeMillis().toString()); if (checkInNow) { put(COL_BOOKING_STATUS, BOOKING_STATUS_CHECKED_IN); put(COL_BOOKING_ACTUAL_CHECKIN, System.currentTimeMillis().toString()) } else { put(COL_BOOKING_STATUS, BOOKING_STATUS_BOOKED) } }
            val bookingId = db.insert(TABLE_BOOKINGS, null, cv)
            if (bookingId != -1L) {
                val newStatus = if (checkInNow) ROOM_STATUS_OCCUPIED else ROOM_STATUS_BOOKED
                val roomCv = ContentValues().apply { 
                    put(COL_ROOM_STATUS, newStatus)
                    put(COL_ROOM_UPDATED_AT, currentDateString())
                }
                db.update(TABLE_ROOMS, roomCv, "$COL_ROOM_NUMBER = ?", arrayOf(roomNumber))
            }
            db.setTransactionSuccessful(); return bookingId
        } finally { db.endTransaction() }
    }

    fun getPendingCheckIns(): List<CheckoutBooking> = getBookingsByStatus(BOOKING_STATUS_BOOKED)

    fun checkIn(bookingId: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply { put(COL_BOOKING_STATUS, BOOKING_STATUS_CHECKED_IN); put(COL_BOOKING_ACTUAL_CHECKIN, System.currentTimeMillis().toString()) }
            val rows = db.update(TABLE_BOOKINGS, cv, "$COL_BOOKING_ID = ?", arrayOf(bookingId.toString()))
            if (rows > 0) {
                val roomNumber = getRoomNumberForBooking(db, bookingId)
                if (roomNumber != null) {
                    val roomCv = ContentValues().apply { 
                        put(COL_ROOM_STATUS, ROOM_STATUS_OCCUPIED)
                        put(COL_ROOM_UPDATED_AT, currentDateString())
                    }
                    db.update(TABLE_ROOMS, roomCv, "$COL_ROOM_NUMBER = ?", arrayOf(roomNumber))
                }
            }
            db.setTransactionSuccessful(); return rows > 0
        } finally { db.endTransaction() }
    }

    fun checkOut(bookingId: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put(COL_BOOKING_STATUS, BOOKING_STATUS_CHECKED_OUT)
                put(COL_BOOKING_ACTUAL_CHECKOUT, System.currentTimeMillis().toString())
            }
            val rows = db.update(TABLE_BOOKINGS, cv, "$COL_BOOKING_ID = ?", arrayOf(bookingId.toString()))
            if (rows > 0) {
                val roomNumber = getRoomNumberForBooking(db, bookingId)
                if (roomNumber != null) {
                    // 1. Cập nhật trạng thái phòng sang Đang dọn
                    val roomCv = ContentValues().apply { 
                        put(COL_ROOM_STATUS, ROOM_STATUS_MAINTENANCE)
                        put(COL_ROOM_UPDATED_AT, currentDateString())
                    }
                    db.update(TABLE_ROOMS, roomCv, "$COL_ROOM_NUMBER = ?", arrayOf(roomNumber))

                    // 2. Tạo nhiệm vụ dọn phòng cho Buồng phòng
                    val taskCv = ContentValues().apply {
                        put(COL_TASK_ROOM_NUMBER, roomNumber)
                        put(COL_TASK_TYPE, DrawerType.CLEAN.name)
                        put(COL_TASK_LABEL, "Dọn phòng $roomNumber sau khi khách trả phòng")
                        put(COL_TASK_PRIORITY, TaskPriority.NORMAL.name)
                        put(COL_TASK_IS_DONE, 0)
                        put(COL_TASK_DATE, currentDateString())
                    }
                    db.insert(TABLE_HOUSEKEEPING_TASKS, null, taskCv)
                }
            }
            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    private fun getRoomNumberForBooking(db: SQLiteDatabase, bookingId: Long): String? {
        val cursor = db.query(TABLE_BOOKINGS, arrayOf(COL_BOOKING_ROOM_NUMBER), "$COL_BOOKING_ID = ?", arrayOf(bookingId.toString()), null, null, null)
        var roomNumber: String? = null
        if (cursor.moveToFirst()) roomNumber = cursor.getString(0)
        cursor.close()
        return roomNumber
    }

    fun getBookingsByStatus(status: String): List<CheckoutBooking> {
        val db = readableDatabase
        val list = mutableListOf<CheckoutBooking>()
        val cursor = db.rawQuery("SELECT b.$COL_BOOKING_ID, b.$COL_BOOKING_ROOM_NUMBER, r.$COL_ROOM_TYPE, c.$COL_CUSTOMER_NAME, c.$COL_CUSTOMER_PHONE, b.$COL_BOOKING_CHECKIN_DATE, b.$COL_BOOKING_CHECKOUT_DATE, b.$COL_BOOKING_PRICE_PER_NIGHT, c.$COL_CUSTOMER_ID_CARD FROM $TABLE_BOOKINGS b JOIN $TABLE_ROOMS r ON r.$COL_ROOM_NUMBER = b.$COL_BOOKING_ROOM_NUMBER JOIN $TABLE_CUSTOMERS c ON c.$COL_CUSTOMER_ID = b.$COL_BOOKING_CUSTOMER_ID WHERE b.$COL_BOOKING_STATUS = ? ORDER BY b.$COL_BOOKING_ID DESC", arrayOf(status))
        while (cursor.moveToNext()) {
            val bookingId = cursor.getLong(0); val checkinDate = cursor.getString(5); val checkoutDate = cursor.getString(6)
            list.add(CheckoutBooking(
                bookingId = bookingId.toString(),
                roomNumber = cursor.getString(1),
                roomType = cursor.getString(2),
                guestName = cursor.getString(3),
                guestPhone = cursor.getString(4) ?: "",
                guestIdCard = cursor.getString(8) ?: "",
                checkInDate = checkinDate,
                checkOutDate = checkoutDate,
                nights = calculateNights(checkinDate, checkoutDate),
                roomPricePerNight = cursor.getLong(7),
                serviceCharges = getServiceCharges(bookingId),
                rawStatus = status
            ))
        }
        cursor.close()
        return list
    }

    fun syncBookings(bookings: List<CheckoutBooking>, status: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (booking in bookings) {
                val guestPhone = booking.guestPhone ?: ""
                var customerId = findCustomerByPhone(guestPhone)
                if (customerId == null) {
                    customerId = addCustomer(booking.guestName ?: "Unknown", guestPhone, booking.guestIdCard)
                }

                val cv = ContentValues().apply {
                    put(COL_BOOKING_ID, booking.bookingId)
                    put(COL_BOOKING_ROOM_NUMBER, booking.roomNumber)
                    put(COL_BOOKING_CUSTOMER_ID, customerId)
                    put(COL_BOOKING_CHECKIN_DATE, booking.checkInDate)
                    put(COL_BOOKING_CHECKOUT_DATE, booking.checkOutDate)
                    put(COL_BOOKING_ACTUAL_CHECKIN, booking.actualCheckin)
                    put(COL_BOOKING_ACTUAL_CHECKOUT, booking.actualCheckout)
                    put(COL_BOOKING_PRICE_PER_NIGHT, booking.roomPricePerNight)
                    put(COL_BOOKING_STATUS, status)
                    put(COL_BOOKING_CREATED_BY, booking.createdBy)
                    put(COL_BOOKING_CREATED_AT, booking.createdAt)
                }
                val rows = db.update(TABLE_BOOKINGS, cv, "$COL_BOOKING_ID = ?", arrayOf(booking.bookingId ?: ""))
                if (rows == 0) {
                    db.insert(TABLE_BOOKINGS, null, cv)
                }

                db.delete(TABLE_SERVICE_CHARGES, "$COL_SERVICE_BOOKING_ID = ?", arrayOf(booking.bookingId ?: ""))
                for (sc in booking.serviceCharges ?: emptyList()) {
                    val scCv = ContentValues().apply {
                        put(COL_SERVICE_BOOKING_ID, booking.bookingId)
                        put(COL_SERVICE_NAME, sc.name)
                        put(COL_SERVICE_AMOUNT, sc.amount)
                    }
                    db.insert(TABLE_SERVICE_CHARGES, null, scCv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun mapCheckoutStatus(bookingStatus: String, checkoutDate: String): CheckoutStatus {
        return when (bookingStatus) { BOOKING_STATUS_CHECKED_OUT -> CheckoutStatus.CHECKED_OUT; else -> { val today = currentDateString(); if (checkoutDate < today) CheckoutStatus.OVERDUE else CheckoutStatus.DUE_TODAY } }
    }

    private fun calculateNights(checkin: String, checkout: String): Int {
        return try { val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()); val d1 = fmt.parse(checkin)!!.time; val d2 = fmt.parse(checkout)!!.time; val diff = (d2 - d1) / (1000L * 60 * 60 * 24); if (diff < 1) 1 else diff.toInt() } catch (e: Exception) { 1 }
    }

    private fun currentDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    fun getServiceCharges(bookingId: Long): List<ServiceCharge> {
        val db = readableDatabase
        val list = mutableListOf<ServiceCharge>()
        val cursor = db.query(TABLE_SERVICE_CHARGES, arrayOf(COL_SERVICE_NAME, COL_SERVICE_AMOUNT), "$COL_SERVICE_BOOKING_ID = ?", arrayOf(bookingId.toString()), null, null, null)
        while (cursor.moveToNext()) { list.add(ServiceCharge(name = cursor.getString(0), amount = cursor.getLong(1))) }
        cursor.close()
        return list
    }

    fun getRevenueByMonth(year: Int, month: Int): Long {
        val db = readableDatabase
        val monthStr = "%04d-%02d".format(year, month)
        var total: Long = 0
        val cursor = db.rawQuery(
            "SELECT $COL_BOOKING_ID, $COL_BOOKING_PRICE_PER_NIGHT, $COL_BOOKING_CHECKIN_DATE, $COL_BOOKING_CHECKOUT_DATE FROM $TABLE_BOOKINGS WHERE $COL_BOOKING_CHECKOUT_DATE LIKE ? AND $COL_BOOKING_STATUS = '$BOOKING_STATUS_CHECKED_OUT'",
            arrayOf("$monthStr%")
        )
        while (cursor.moveToNext()) {
            val bookingId = cursor.getLong(0)
            val pricePerNight = cursor.getLong(1)
            val checkin = cursor.getString(2)
            val checkout = cursor.getString(3)
            val nights = calculateNights(checkin, checkout)
            total += (pricePerNight * nights)
            val scCursor = db.rawQuery("SELECT SUM($COL_SERVICE_AMOUNT) FROM $TABLE_SERVICE_CHARGES WHERE $COL_SERVICE_BOOKING_ID = ?", arrayOf(bookingId.toString()))
            if (scCursor.moveToFirst()) total += scCursor.getLong(0)
            scCursor.close()
        }
        cursor.close()
        return total
    }

    // =====================================================================
    // HOUSEKEEPING TASKS
    // =====================================================================

    fun getTasksByType(date: String, type: DrawerType, assignedTo: String? = null): List<TaskItem> {
        val db = readableDatabase
        val list = mutableListOf<TaskItem>()
        // Lấy task của ngày hôm nay HOẶC các task cũ chưa hoàn thành
        val whereClauses = mutableListOf("(($COL_TASK_DATE = ?) OR ($COL_TASK_DATE < ? AND $COL_TASK_IS_DONE = 0))", "$COL_TASK_TYPE = ?")
        val args = mutableListOf(date, date, type.name)
        if (assignedTo != null) { 
            whereClauses.add("$COL_TASK_ASSIGNED_TO = ?")
            args.add(assignedTo) 
        }
        val cursor = db.rawQuery("SELECT t.$COL_TASK_ID, t.$COL_TASK_ROOM_NUMBER, t.$COL_TASK_LABEL, r.$COL_ROOM_TIER, t.$COL_TASK_PRIORITY, t.$COL_TASK_IS_DONE FROM $TABLE_HOUSEKEEPING_TASKS t JOIN $TABLE_ROOMS r ON r.$COL_ROOM_NUMBER = t.$COL_TASK_ROOM_NUMBER WHERE ${whereClauses.joinToString(" AND ")} ORDER BY t.$COL_TASK_ID ASC", args.toTypedArray())
        while (cursor.moveToNext()) { 
            list.add(TaskItem(id = cursor.getLong(0).toString(), label = cursor.getString(2) ?: "Phòng ${cursor.getString(1)}", tier = RoomTier.valueOf(cursor.getString(3)), priority = TaskPriority.valueOf(cursor.getString(4)), isDone = cursor.getInt(5) == 1)) 
        }
        cursor.close()
        return list
    }

    fun setTaskDone(taskId: Long, isDone: Boolean): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put(COL_TASK_IS_DONE, if (isDone) 1 else 0)
                if (isDone) put(COL_TASK_PRIORITY, TaskPriority.DONE.name)
            }
            val rows = db.update(TABLE_HOUSEKEEPING_TASKS, cv, "$COL_TASK_ID = ?", arrayOf(taskId.toString()))
            
            if (rows > 0 && isDone) {
                // Lấy số phòng của task này
                val cursor = db.query(TABLE_HOUSEKEEPING_TASKS, arrayOf(COL_TASK_ROOM_NUMBER), "$COL_TASK_ID = ?", arrayOf(taskId.toString()), null, null, null)
                if (cursor.moveToFirst()) {
                    val roomNumber = cursor.getString(0)
                    
                    // Kiểm tra xem phòng này có đang bị báo cáo sửa chữa (PENDING/IN_PROGRESS) không
                    val reportCursor = db.query(TABLE_MAINTENANCE_REPORTS, arrayOf(COL_REPORT_ID), 
                        "$COL_REPORT_ROOM_NUMBER = ? AND $COL_REPORT_STATUS != ?", 
                        arrayOf(roomNumber, REPORT_STATUS_DONE), null, null, null)
                    
                    val hasPendingMaintenance = reportCursor.moveToFirst()
                    reportCursor.close()
                    
                    // Nếu không có báo cáo sửa chữa chưa xong, trả phòng về trạng thái AVAILABLE
                    if (!hasPendingMaintenance) {
                        val roomCv = ContentValues().apply { 
                            put(COL_ROOM_STATUS, ROOM_STATUS_AVAILABLE)
                            put(COL_ROOM_UPDATED_AT, currentDateString())
                        }
                        db.update(TABLE_ROOMS, roomCv, "$COL_ROOM_NUMBER = ?", arrayOf(roomNumber))
                    }
                }
                cursor.close()
            }
            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    fun getTaskSummary(date: String, assignedTo: String? = null): Pair<Int, Int> {
        val db = readableDatabase
        val args = mutableListOf(date, date)
        var where = "(($COL_TASK_DATE = ?) OR ($COL_TASK_DATE < ? AND $COL_TASK_IS_DONE = 0))"
        if (assignedTo != null) { 
            where += " AND $COL_TASK_ASSIGNED_TO = ?"
            args.add(assignedTo) 
        }
        val cursor = db.rawQuery("SELECT COUNT(*), SUM($COL_TASK_IS_DONE) FROM $TABLE_HOUSEKEEPING_TASKS WHERE $where", args.toTypedArray())
        var total = 0; var done = 0
        if (cursor.moveToFirst()) { total = cursor.getInt(0); done = if (cursor.isNull(1)) 0 else cursor.getInt(1) }
        cursor.close()
        return Pair(total, done)
    }

    fun checkPeriodicCleaning() {
        val db = writableDatabase
        val today = currentDateString()
        
        // Tìm các phòng AVAILABLE đã quá 7 ngày chưa có thay đổi
        val cursor = db.query(TABLE_ROOMS, arrayOf(COL_ROOM_NUMBER), 
            "$COL_ROOM_STATUS = ? AND $COL_ROOM_UPDATED_AT <= ?", 
            arrayOf(ROOM_STATUS_AVAILABLE, getDateDaysAgo(7)), null, null, null)
            
        val roomsToClean = mutableListOf<String>()
        while (cursor.moveToNext()) {
            roomsToClean.add(cursor.getString(0))
        }
        cursor.close()
        
        if (roomsToClean.isEmpty()) return
        
        db.beginTransaction()
        try {
            for (roomNumber in roomsToClean) {
                // 1. Chuyển trạng thái phòng
                val roomCv = ContentValues().apply {
                    put(COL_ROOM_STATUS, ROOM_STATUS_MAINTENANCE)
                    put(COL_ROOM_UPDATED_AT, today)
                }
                db.update(TABLE_ROOMS, roomCv, "$COL_ROOM_NUMBER = ?", arrayOf(roomNumber))
                
                // 2. Tạo task dọn dẹp định kỳ
                val taskCv = ContentValues().apply {
                    put(COL_TASK_ROOM_NUMBER, roomNumber)
                    put(COL_TASK_TYPE, DrawerType.CLEAN.name)
                    put(COL_TASK_LABEL, "Vệ sinh định kỳ (Phòng trống > 7 ngày)")
                    put(COL_TASK_PRIORITY, TaskPriority.NORMAL.name)
                    put(COL_TASK_IS_DONE, 0)
                    put(COL_TASK_DATE, today)
                }
                db.insert(TABLE_HOUSEKEEPING_TASKS, null, taskCv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
    
    private fun getDateDaysAgo(days: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -days)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
    }

    // =====================================================================
    // MAINTENANCE REPORTS
    // =====================================================================

    fun addMaintenanceReport(roomNumber: String, description: String, reportedBy: String, imagePath: String? = null): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply { 
                put(COL_REPORT_ROOM_NUMBER, roomNumber)
                put(COL_REPORT_DESCRIPTION, description)
                put(COL_REPORT_REPORTED_BY, reportedBy)
                put(COL_REPORT_STATUS, REPORT_STATUS_PENDING)
                put(COL_REPORT_CREATED_AT, System.currentTimeMillis().toString())
                put(COL_REPORT_IMAGE_PATH, imagePath)
            }
            val id = db.insert(TABLE_MAINTENANCE_REPORTS, null, cv)
            val roomCv = ContentValues().apply { 
                put(COL_ROOM_STATUS, ROOM_STATUS_MAINTENANCE)
                put(COL_ROOM_UPDATED_AT, currentDateString())
            }
            db.update(TABLE_ROOMS, roomCv, "$COL_ROOM_NUMBER = ?", arrayOf(roomNumber))
            db.setTransactionSuccessful(); return id != -1L
        } finally { db.endTransaction() }
    }

    fun getMaintenanceReports(status: String? = null): List<MaintenanceReportItem> {
        val db = readableDatabase
        val list = mutableListOf<MaintenanceReportItem>()
        val cols = arrayOf(COL_REPORT_ID, COL_REPORT_ROOM_NUMBER, COL_REPORT_DESCRIPTION, COL_REPORT_REPORTED_BY, COL_REPORT_STATUS, COL_REPORT_CREATED_AT, COL_REPORT_IMAGE_PATH)
        val cursor = if (status != null) { db.query(TABLE_MAINTENANCE_REPORTS, cols, "$COL_REPORT_STATUS = ?", arrayOf(status), null, null, "$COL_REPORT_ID DESC") } else { db.query(TABLE_MAINTENANCE_REPORTS, cols, null, null, null, null, "$COL_REPORT_ID DESC") }
        while (cursor.moveToNext()) { 
            list.add(MaintenanceReportItem(
                reportId = cursor.getLong(0), 
                roomNumber = cursor.getString(1), 
                description = cursor.getString(2), 
                reportedBy = cursor.getString(3), 
                status = cursor.getString(4), 
                createdAt = cursor.getString(5),
                imagePath = cursor.getString(6)
            )) 
        }
        cursor.close()
        return list
    }

    fun updateMaintenanceReportStatus(reportId: Long, status: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply { put(COL_REPORT_STATUS, status) }
            val rows = db.update(TABLE_MAINTENANCE_REPORTS, cv, "$COL_REPORT_ID = ?", arrayOf(reportId.toString()))
            if (rows > 0 && status == REPORT_STATUS_DONE) {
                val cursor = db.query(TABLE_MAINTENANCE_REPORTS, arrayOf(COL_REPORT_ROOM_NUMBER), "$COL_REPORT_ID = ?", arrayOf(reportId.toString()), null, null, null)
                if (cursor.moveToFirst()) { 
                    val roomNumber = cursor.getString(0)
                    val roomCv = ContentValues().apply { 
                        put(COL_ROOM_STATUS, ROOM_STATUS_AVAILABLE)
                        put(COL_ROOM_UPDATED_AT, currentDateString())
                    }
                    db.update(TABLE_ROOMS, roomCv, "$COL_ROOM_NUMBER = ?", arrayOf(roomNumber)) 
                }
                cursor.close()
            }
            db.setTransactionSuccessful(); return rows > 0
        } finally { db.endTransaction() }
    }

    // =====================================================================
    // SYNC QUEUE
    // =====================================================================

    fun addToSyncQueue(actionType: String, targetId: String, payload: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_SYNC_ACTION_TYPE, actionType)
            put(COL_SYNC_TARGET_ID, targetId)
            put(COL_SYNC_PAYLOAD, payload)
            put(COL_SYNC_CREATED_AT, System.currentTimeMillis().toString())
            put(COL_SYNC_STATUS, SYNC_STATUS_PENDING)
            put(COL_SYNC_RETRY_COUNT, 0)
        }
        return db.insert(TABLE_SYNC_QUEUE, null, cv)
    }

    fun getPendingSyncItems(): List<SyncQueueItem> {
        val db = readableDatabase
        val list = mutableListOf<SyncQueueItem>()
        val cursor = db.query(
            TABLE_SYNC_QUEUE,
            null,
            "$COL_SYNC_STATUS = ?",
            arrayOf(SYNC_STATUS_PENDING),
            null,
            null,
            "$COL_SYNC_CREATED_AT ASC"
        )
        
        cursor?.use {
            val idIdx = it.getColumnIndex(COL_SYNC_ID)
            val typeIdx = it.getColumnIndex(COL_SYNC_ACTION_TYPE)
            val targetIdx = it.getColumnIndex(COL_SYNC_TARGET_ID)
            val payloadIdx = it.getColumnIndex(COL_SYNC_PAYLOAD)
            val createdIdx = it.getColumnIndex(COL_SYNC_CREATED_AT)
            val retryIdx = it.getColumnIndex(COL_SYNC_RETRY_COUNT)

            while (it.moveToNext()) {
                if (idIdx != -1 && typeIdx != -1 && targetIdx != -1) {
                    list.add(SyncQueueItem(
                        id = it.getLong(idIdx),
                        actionType = it.getString(typeIdx),
                        targetId = it.getString(targetIdx),
                        payload = if (payloadIdx != -1) it.getString(payloadIdx) ?: "" else "",
                        createdAt = if (createdIdx != -1) it.getString(createdIdx) ?: "" else "",
                        retryCount = if (retryIdx != -1) it.getInt(retryIdx) else 0
                    ))
                }
            }
        }
        return list
    }

    fun markSyncItemDone(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_SYNC_QUEUE, "$COL_SYNC_ID = ?", arrayOf(id.toString()))
    }

    fun incrementSyncRetry(id: Long) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_SYNC_QUEUE SET $COL_SYNC_RETRY_COUNT = $COL_SYNC_RETRY_COUNT + 1 WHERE $COL_SYNC_ID = $id")
    }

    // =====================================================================
    // CALENDAR - RoomCalendarActivity
    // =====================================================================

    fun getRoomCountsForDate(date: String): Triple<Int, Int, Int> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(DISTINCT b.$COL_BOOKING_ROOM_NUMBER) FROM $TABLE_BOOKINGS b WHERE b.$COL_BOOKING_STATUS != 'CANCELLED' AND b.$COL_BOOKING_CHECKIN_DATE <= ? AND b.$COL_BOOKING_CHECKOUT_DATE > ?", arrayOf(date, date))
        var occupied = 0; if (cursor.moveToFirst()) occupied = cursor.getInt(0); cursor.close()
        val maintenance = if (date == currentDateString()) { getRoomStatusCounts()[ROOM_STATUS_MAINTENANCE] ?: 0 } else 0
        val totalRooms = getTotalRoomCount()
        val available = (totalRooms - occupied - maintenance).coerceAtLeast(0)
        return Triple(available, occupied, maintenance)
    }

    private fun getTotalRoomCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_ROOMS", null)
        var count = 0; if (cursor.moveToFirst()) count = cursor.getInt(0); cursor.close(); return count
    }

    fun syncTasks(tasks: List<TaskItem>, date: String, type: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (task in tasks) {
                val roomNumber = task.label.replace("Phòng ", "").trim()
                val cv = ContentValues().apply {
                    put(COL_TASK_ID, task.id as String)
                    put(COL_TASK_ROOM_NUMBER, roomNumber)
                    put(COL_TASK_TYPE, type)
                    put(COL_TASK_LABEL, task.label)
                    put(COL_TASK_PRIORITY, task.priority.name)
                    put(COL_TASK_IS_DONE, if (task.isDone) 1 else 0)
                    put(COL_TASK_DATE, date)
                }
                val rows = db.update(TABLE_HOUSEKEEPING_TASKS, cv, "$COL_TASK_ID = ?", arrayOf(task.id))
                if (rows == 0) {
                    db.insert(TABLE_HOUSEKEEPING_TASKS, null, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun syncMaintenanceReports(reports: List<MaintenanceReportItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (report in reports) {
                val cv = ContentValues().apply {
                    put(COL_REPORT_ID, report.reportId)
                    put(COL_REPORT_ROOM_NUMBER, report.roomNumber)
                    put(COL_REPORT_DESCRIPTION, report.description)
                    put(COL_REPORT_REPORTED_BY, report.reportedBy)
                    put(COL_REPORT_STATUS, report.status)
                    put(COL_REPORT_CREATED_AT, report.createdAt)
                    put(COL_REPORT_IMAGE_PATH, report.imagePath)
                }
                val rows = db.update(TABLE_MAINTENANCE_REPORTS, cv, "$COL_REPORT_ID = ?", arrayOf(report.reportId.toString()))
                if (rows == 0) {
                    db.insert(TABLE_MAINTENANCE_REPORTS, null, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    data class MaintenanceReportItem(
        @SerializedName("reportId") val reportId: Long,
        @SerializedName("roomNumber") val roomNumber: String,
        @SerializedName("description") val description: String,
        @SerializedName("reportedBy") val reportedBy: String?,
        @SerializedName("status") val status: String,
        @SerializedName("createdAt") val createdAt: String?,
        @SerializedName("imagePath") val imagePath: String? = null
    )

    data class SyncQueueItem(val id: Long, val actionType: String, val targetId: String, val payload: String, val createdAt: String, val retryCount: Int)
}
