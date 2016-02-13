package com.google.android.gms.games.multiplayer;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer.Std;
import com.google.android.gms.common.internal.safeparcel.C0261a;
import com.google.android.gms.common.internal.safeparcel.C0261a.C0260a;
import com.google.android.gms.common.internal.safeparcel.C0262b;
import com.google.android.gms.location.GeofenceStatusCodes;

public class ParticipantResultCreator implements Creator<ParticipantResult> {
    public static final int CONTENT_DESCRIPTION = 0;

    static void m577a(ParticipantResult participantResult, Parcel parcel, int i) {
        int p = C0262b.m236p(parcel);
        C0262b.m222a(parcel, 1, participantResult.getParticipantId(), false);
        C0262b.m234c(parcel, GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE, participantResult.getVersionCode());
        C0262b.m234c(parcel, 2, participantResult.getResult());
        C0262b.m234c(parcel, 3, participantResult.getPlacing());
        C0262b.m211F(parcel, p);
    }

    public ParticipantResult createFromParcel(Parcel parcel) {
        int i = 0;
        int o = C0261a.m196o(parcel);
        String str = null;
        int i2 = 0;
        int i3 = 0;
        while (parcel.dataPosition() < o) {
            int n = C0261a.m194n(parcel);
            switch (C0261a.m174R(n)) {
                case Std.STD_FILE /*1*/:
                    str = C0261a.m195n(parcel, n);
                    break;
                case Std.STD_URL /*2*/:
                    i2 = C0261a.m187g(parcel, n);
                    break;
                case Std.STD_URI /*3*/:
                    i = C0261a.m187g(parcel, n);
                    break;
                case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE /*1000*/:
                    i3 = C0261a.m187g(parcel, n);
                    break;
                default:
                    C0261a.m180b(parcel, n);
                    break;
            }
        }
        if (parcel.dataPosition() == o) {
            return new ParticipantResult(i3, str, i2, i);
        }
        throw new C0260a("Overread allowed size end=" + o, parcel);
    }

    public ParticipantResult[] newArray(int size) {
        return new ParticipantResult[size];
    }
}
