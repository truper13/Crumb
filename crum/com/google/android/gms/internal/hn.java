package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer.Std;
import com.google.android.gms.common.internal.safeparcel.C0261a;
import com.google.android.gms.common.internal.safeparcel.C0261a.C0260a;
import com.google.android.gms.common.internal.safeparcel.C0262b;
import com.google.android.gms.location.GeofenceStatusCodes;

public class hn implements Creator<hm> {
    static void m1070a(hm hmVar, Parcel parcel, int i) {
        int p = C0262b.m236p(parcel);
        C0262b.m222a(parcel, 1, hmVar.Rd, false);
        C0262b.m234c(parcel, GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE, hmVar.xH);
        C0262b.m211F(parcel, p);
    }

    public hm aG(Parcel parcel) {
        int o = C0261a.m196o(parcel);
        int i = 0;
        String str = null;
        while (parcel.dataPosition() < o) {
            int n = C0261a.m194n(parcel);
            switch (C0261a.m174R(n)) {
                case Std.STD_FILE /*1*/:
                    str = C0261a.m195n(parcel, n);
                    break;
                case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE /*1000*/:
                    i = C0261a.m187g(parcel, n);
                    break;
                default:
                    C0261a.m180b(parcel, n);
                    break;
            }
        }
        if (parcel.dataPosition() == o) {
            return new hm(i, str);
        }
        throw new C0260a("Overread allowed size end=" + o, parcel);
    }

    public hm[] bH(int i) {
        return new hm[i];
    }

    public /* synthetic */ Object createFromParcel(Parcel x0) {
        return aG(x0);
    }

    public /* synthetic */ Object[] newArray(int x0) {
        return bH(x0);
    }
}
