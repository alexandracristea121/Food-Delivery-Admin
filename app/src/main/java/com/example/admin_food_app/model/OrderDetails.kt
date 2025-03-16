package com.example.admin_food_app.model

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.util.ArrayList

class OrderDetails() : Serializable {

    var adminUserId: String? = null
    var userUid: String? = null
    var userName: String? = null
    var foodNames: MutableList<String>? = null
    var foodImages: MutableList<String>? = null
    var foodPrices: MutableList<String>? = null
    var foodQuantities: MutableList<Int>? = null
    var userLocation: String? = null
    var restaurantLocation: String? = null
    var totalPrice: String? = null
    var phoneNumber: String? = null
    var orderAccepted: Boolean = false
    var paymentReceived: Boolean = false
    var itemPushkey: String? = null
    var orderTime: String? = null
    var orderDelivered: Boolean = false

    constructor(
        userId: String,
        name: String,
        foodItemName: ArrayList<String>,
        foodItemPrice: ArrayList<String>,
        foodItemImage: ArrayList<String>,
        foodItemQuantities: ArrayList<Int>,
        userLocation: String,
        restaurantLocation: String,
        totalAmount: String,
        phone: String,
        time: String,
        itemPushKey: String?,
        orderAccepted: Boolean,
        paymentReceived: Boolean,
        adminUserId: String?,
        orderDelivered: Boolean
    ) : this() {
        this.userUid = userId
        this.userName = name
        this.foodNames = foodItemName
        this.foodPrices = foodItemPrice
        this.foodImages = foodItemImage
        this.foodQuantities = foodItemQuantities
        this.userLocation = userLocation
        this.restaurantLocation = restaurantLocation
        this.totalPrice = totalAmount
        this.phoneNumber = phone
        this.orderTime = time
        this.itemPushkey = itemPushKey
        this.orderAccepted = orderAccepted
        this.paymentReceived = paymentReceived
        this.adminUserId = adminUserId
        this.orderDelivered = orderDelivered
    }

    fun describeContents(): Int {
        return 0
    }

    fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userUid)
        parcel.writeString(userName)
        parcel.writeStringList(foodNames)
        parcel.writeStringList(foodImages)
        parcel.writeStringList(foodPrices)
        parcel.writeList(foodQuantities)
        parcel.writeString(totalPrice)
        parcel.writeString(phoneNumber)
        parcel.writeByte(if (orderAccepted) 1 else 0)
        parcel.writeByte(if (orderDelivered) 1 else 0)
        parcel.writeByte(if (paymentReceived) 1 else 0)
        parcel.writeString(itemPushkey)
        parcel.writeString(orderTime)
    }
}
