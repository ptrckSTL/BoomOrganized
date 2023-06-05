package com.simplemobiletools.boomorganized.sheets

import android.os.Parcel
import android.os.Parcelable

sealed class FilterableValue : Parcelable {
    abstract val value: String

    class Include(override val value: String) : FilterableValue() {
        constructor(parcel: Parcel) : this(parcel.readString() ?: "")

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(value)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Include> {
            override fun createFromParcel(parcel: Parcel): Include {
                return Include(parcel)
            }

            override fun newArray(size: Int): Array<Include?> {
                return arrayOfNulls(size)
            }
        }
    }

    class Exclude(override val value: String) : FilterableValue() {
        constructor(parcel: Parcel) : this(parcel.readString() ?: "")

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(value)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Exclude> {
            override fun createFromParcel(parcel: Parcel): Exclude {
                return Exclude(parcel)
            }

            override fun newArray(size: Int): Array<Exclude?> {
                return arrayOfNulls(size)
            }
        }
    }

    class None(override val value: String) : FilterableValue() {
        constructor(parcel: Parcel) : this(parcel.readString() ?: "")

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(value)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<None> {
            override fun createFromParcel(parcel: Parcel): None {
                return None(parcel)
            }

            override fun newArray(size: Int): Array<None?> {
                return arrayOf()
            }
        }
    }
}
