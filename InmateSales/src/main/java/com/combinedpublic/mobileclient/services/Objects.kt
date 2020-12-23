package com.combinedpublic.mobileclient.services

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject

class iceObject(val url: String?, val username: String?, val credential: String?)
class offer(val type: String?,
            val id: Long?, val dst: Long?, val conversationid: Long?,
            val payload: offerPayload)
class offer_rcv(val type: String?, val param0: String?,val param1: String?,
                val param3: String?,val status: String?,
                val displayname: String?, val statusdesc: String?,
                val extensionid: String?, val personid: String?, val device: String?,
            val id: Long?, val dst: Long?, val conversationid: Long?,
            val userid: Long?, val inmateid: Long?, val visitationid: Long?,
            val payload: offerPayload? )
class answer_rcv(val type: String?, val id: Long?, val dst: Long?, val conversationid: Long?,
                 val inmateid: Long?, val visitationid: Long?,
                val payload: offerPayload? )
class answer(val type: String?,
            val id: Long?, val dst: Long?, val conversationid: Long?,
            val payload: offerPayload?)

class offerPayload(val sdp: String?, val type: String?)

class ra_answer(val type: String?,
             val id: Long?, val dst: Long?, val conversationid: Long?,
             val payload: JSONObject? )
class ra_answer_snd(val type: String?, val param2: String?, val id: Long?, val dst: Long?, val conversationid: Long?,
                    val status: String?)

class deviceinfo(val type: String?, val device: String?, val param0: String?, val param1: String?, val id: String?)
class register_name(val type: String?, val userid: String?, val device: String?, val param0: String?, val id: Int?, val param2: String?, val param3: String?)
class registerresult(val type: String?, val userid: String?, val status: String?, val id: String?, val displayname: String?, val balance: String?, val param2: String?)
class registerresultdesc(val type: String?, val userid: String?, val status: String?, val statusdesc: String?)
class getpeerlist(val type: String?, val device: String?, val userid: String?, val id: String?)
class hangup(val type: String?, val device: String?, val id: String?, val dst: String?, val conversationid: String?)
class ra_call(val type: String?, val device: String?, var token: String?, var param2: String?, val id: String?, val dst: String?)
class ra_callresult(val type: String?, val id: String?, val dst: String?, val conversationid: String?,
                    val status: String?, val param0: String?, val param1: String?,
                    val param2: String?, val param3: String?, val displayname: String?)
class ra_callresult_error(val type: String?, val conversationid: String?, val status: String?, val statusdesc: String?)//val statusdesc: String?,
class getpeerlistresult(val type: String?, val status: String?, val param2: String?, val id: String?, val balance: String?, val payload: ArrayList<Contact>)

class ra_reject(val type: String?, val payload: String?, val param0: String?, val param1: String?,
                val param3: String?, val status: String?, val displayname: String?,
                val statusdesc: String?, val extensionid: String?, val personid: String?,val device: String?,
                val id: Long?, val dst: Long?,
                val conversationid: Long?, val userid: Long?, val inmateid: Long?,
                val visitationid: Long?)
class ra_answer_rcv(val type: String?, val payload: String?, val param0: String?, val param1: String?,
                    var param2: String?,
                val param3: String?, val status: String?, val displayname: String?,
                val statusdesc: String?, val extensionid: String?, val personid: String?,val device: String?,
                val id: Long?, val dst: Long?,
                val conversationid: Long?, val userid: Long?, val inmateid: Long?,
                val visitationid: Long?)

class call(val type: String?, val id: Long?,
                        val dst: Long?, val conversationid: Long?)

class callresult(val type: String?, val status: String?, val id: Long?,
           val dst: Long?, val conversationid: String?)


class keepalive(val type: String?, val device: String?, val id: Long?,
                 val conversationid: Long?)

class callconnected(val type: String?, val id: String?, val dst: Long?,
                val conversationid: Long?)

class keepaliveresult(val type: String?)

class gettoken(val type: String?, val displayname: String?, val id: Long?, val conversationid: Long?)

class gettokenresult(val type: String?, val status: String?, val payload: String?, val param0: String?)


class candidate(val type: String?, val param0: String?, val status: String?,
                val displayname: String?, val statusdesc: String?, val extensionid: String?, val personid: String?,
                val device: String?, val param1: String?, val param2: String?, val param3: String?,
                val id: Long?, val dst: Long?, val conversationid: Long?, val userid: Long?,
                val inmateid: Long?, val visitationid: Long?,
                val payload: Payload?)

class newCandidate(val type: String?, val param0: String?, val status: String?,
                val displayname: String?, val statusdesc: String?, val extensionid: String?, val personid: String?,
                val device: String?, val param1: String?, val param2: String?, val param3: String?,
                val id: Long?, val dst: Long?, val conversationid: Long?, val userid: Long?,
                val inmateid: Long?, val visitationid: Long?,
                val payload: candidatePayload?)

class candidatePayload(val candidate: String?, val sdpMid: String?, val sdpMLineIndex: Int?) //, val usernameFragment: String?

// class candidatePayload(val candidate: String?, val sdpMid: Long?, val sdpMLineIndex: Long?, val usernameFragment: String?)

class newCandidateWithSdpMidIsString(val type: String?, val param0: String?, val status: String?,
                   val displayname: String?, val statusdesc: String?, val extensionid: String?, val personid: String?,
                   val device: String?, val param1: String?, val param2: String?, val param3: String?,
                   val id: Long?, val dst: Long?, val conversationid: Long?, val userid: Long?,
                   val inmateid: Long?, val visitationid: Long?,
                   val payload: candidatePayloadWithSdpMidIsString?)

class candidatePayloadWithSdpMidIsString(val candidate: String?, val sdpMid: String?, val sdpMLineIndex: Long?, val usernameFragment: String?)

class Payload(val candidate: String?, val sdpMid: String?, val sdpMLineIndex: Long?)


class ra_call_rcv(val type: String?, val conversationid: String?, val dst: String?, val id: String?, val displayname: String?, val visitationid: String?, val status: String?, val param0: String?, val param1: String?, val param2: String?, val param3: String?) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeString(conversationid)
        parcel.writeString(dst)
        parcel.writeString(id)
        parcel.writeString(displayname)
        parcel.writeString(visitationid)
        parcel.writeString(status)
        parcel.writeString(param0)
        parcel.writeString(param1)
        parcel.writeString(param2)
        parcel.writeString(param3)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ra_call_rcv> {
        override fun createFromParcel(parcel: Parcel): ra_call_rcv {
            return ra_call_rcv(parcel)
        }

        override fun newArray(size: Int): Array<ra_call_rcv?> {
            return arrayOfNulls(size)
        }
    }
}

class bye(val id: String?, val device: String?)

class Contact(var displayname: String?, val id: Long?) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readLong())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(displayname)
        parcel.writeLong(id!!)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Contact> {
        override fun createFromParcel(parcel: Parcel): Contact {
            return Contact(parcel)
        }

        override fun newArray(size: Int): Array<Contact?> {
            return arrayOfNulls(size)
        }
    }
}


