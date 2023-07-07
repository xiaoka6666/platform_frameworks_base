/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.speech;

import android.annotation.NonNull;
import android.os.Parcelable;

import com.android.internal.util.DataClass;

import java.util.ArrayList;
import java.util.List;

/**
 * List of {@link AlternativeSpan} for a specific speech recognition result.
 *
 * <p> A single {@link SpeechRecognizer} result is represented as a {@link String}. Each element
 * in this list is an {@link AlternativeSpan} object representing alternative hypotheses for a
 * specific span (substring) of the originally recognized string.
 */
@DataClass(
        genEqualsHashCode = true,
        genParcelable = true,
        genToString = true
)
public final class AlternativeSpans implements Parcelable {
    /** List of {@link AlternativeSpan} for a specific speech recognition result. */
    @NonNull
    @DataClass.PluralOf("span")
    private final List<AlternativeSpan> mSpans;



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/frameworks/base/core/java/android/speech/AlternativeSpans.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    /**
     * Creates a new AlternativeSpans.
     *
     * @param spans
     *   List of {@link AlternativeSpan} for a specific speech recognition result.
     */
    @DataClass.Generated.Member
    public AlternativeSpans(
            @NonNull List<AlternativeSpan> spans) {
        this.mSpans = spans;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mSpans);

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * List of {@link AlternativeSpan} for a specific speech recognition result.
     */
    @DataClass.Generated.Member
    public @NonNull List<AlternativeSpan> getSpans() {
        return mSpans;
    }

    @Override
    @DataClass.Generated.Member
    public String toString() {
        // You can override field toString logic by defining methods like:
        // String fieldNameToString() { ... }

        return "AlternativeSpans { " +
                "spans = " + mSpans +
        " }";
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@android.annotation.Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(AlternativeSpans other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        AlternativeSpans that = (AlternativeSpans) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mSpans, that.mSpans);
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mSpans);
        return _hash;
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@NonNull android.os.Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        dest.writeParcelableList(mSpans, flags);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    /* package-private */ AlternativeSpans(@NonNull android.os.Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        List<AlternativeSpan> spans = new ArrayList<>();
        in.readParcelableList(spans, AlternativeSpan.class.getClassLoader(), android.speech.AlternativeSpan.class);

        this.mSpans = spans;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mSpans);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public static final @NonNull Parcelable.Creator<AlternativeSpans> CREATOR
            = new Parcelable.Creator<AlternativeSpans>() {
        @Override
        public AlternativeSpans[] newArray(int size) {
            return new AlternativeSpans[size];
        }

        @Override
        public AlternativeSpans createFromParcel(@NonNull android.os.Parcel in) {
            return new AlternativeSpans(in);
        }
    };

    @DataClass.Generated(
            time = 1656603476918L,
            codegenVersion = "1.0.23",
            sourceFile = "frameworks/base/core/java/android/speech/AlternativeSpans.java",
            inputSignatures = "private final @android.annotation.NonNull @com.android.internal.util.DataClass.PluralOf(\"span\") java.util.List<android.speech.AlternativeSpan> mSpans\nclass AlternativeSpans extends java.lang.Object implements [android.os.Parcelable]\n@com.android.internal.util.DataClass(genEqualsHashCode=true, genParcelable=true, genToString=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
