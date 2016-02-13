package com.google.android.gms.internal;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer.Std;
import com.google.android.gms.common.internal.safeparcel.C0261a;
import com.google.android.gms.common.internal.safeparcel.C0261a.C0260a;
import com.google.android.gms.common.internal.safeparcel.C0262b;

public class hj implements Creator<hi> {
    static void m1068a(hi hiVar, Parcel parcel, int i) {
        int p = C0262b.m236p(parcel);
        C0262b.m234c(parcel, 1, hiVar.xH);
        C0262b.m222a(parcel, 2, hiVar.hY(), false);
        C0262b.m222a(parcel, 3, hiVar.getTag(), false);
        C0262b.m211F(parcel, p);
    }

    public hi aE(Parcel parcel) {
        String str = null;
        int o = C0261a.m196o(parcel);
        int i = 0;
        String str2 = null;
        while (parcel.dataPosition() < o) {
            int n = C0261a.m194n(parcel);
            switch (C0261a.m174R(n)) {
                case Std.STD_FILE /*1*/:
                    i = C0261a.m187g(parcel, n);
                    break;
                case Std.STD_URL /*2*/:
                    str2 = C0261a.m195n(parcel, n);
                    break;
                case Std.STD_URI /*3*/:
                    str = C0261a.m195n(parcel, n);
                    break;
                default:
                    C0261a.m180b(parcel, n);
                    break;
            }
        }
        if (parcel.dataPosition() == o) {
            return new hi(i, str2, str);
        }
        throw new C0260a("Overread allowed size end=" + o, parcel);
    }

    public hi[] bF(int i) {
        return new hi[i];
    }

    public /* synthetic */ Object createFromParcel(Parcel x0) {
        return aE(x0);
    }

    public /* synthetic */ Object[] newArray(int x0) {
        return bF(x0);
    }
}
