package com.google.android.gms.drive.events;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer.Std;
import com.google.android.gms.common.internal.safeparcel.C0261a;
import com.google.android.gms.common.internal.safeparcel.C0261a.C0260a;
import com.google.android.gms.common.internal.safeparcel.C0262b;
import com.google.android.gms.drive.DriveId;

/* renamed from: com.google.android.gms.drive.events.b */
public class C0269b implements Creator<ConflictEvent> {
    static void m245a(ConflictEvent conflictEvent, Parcel parcel, int i) {
        int p = C0262b.m236p(parcel);
        C0262b.m234c(parcel, 1, conflictEvent.xH);
        C0262b.m219a(parcel, 2, conflictEvent.Ew, i, false);
        C0262b.m211F(parcel, p);
    }

    public ConflictEvent m246B(Parcel parcel) {
        int o = C0261a.m196o(parcel);
        int i = 0;
        DriveId driveId = null;
        while (parcel.dataPosition() < o) {
            int n = C0261a.m194n(parcel);
            switch (C0261a.m174R(n)) {
                case Std.STD_FILE /*1*/:
                    i = C0261a.m187g(parcel, n);
                    break;
                case Std.STD_URL /*2*/:
                    driveId = (DriveId) C0261a.m176a(parcel, n, DriveId.CREATOR);
                    break;
                default:
                    C0261a.m180b(parcel, n);
                    break;
            }
        }
        if (parcel.dataPosition() == o) {
            return new ConflictEvent(i, driveId);
        }
        throw new C0260a("Overread allowed size end=" + o, parcel);
    }

    public ConflictEvent[] af(int i) {
        return new ConflictEvent[i];
    }

    public /* synthetic */ Object createFromParcel(Parcel x0) {
        return m246B(x0);
    }

    public /* synthetic */ Object[] newArray(int x0) {
        return af(x0);
    }
}
