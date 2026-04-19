package com.favalabs.divvy.ui.groupdetail

import android.net.Uri

fun buildGroupInviteLink(groupId: String, groupName: String): String {
    return Uri.Builder()
        .scheme("https")
        .authority("kfwiircrnpvpnebeeckd.supabase.co")
        .appendPath("functions")
        .appendPath("v1")
        .appendPath("join-group")
        .appendQueryParameter("groupId", groupId)
        .appendQueryParameter("groupName", groupName)
        .build()
        .toString()
}
