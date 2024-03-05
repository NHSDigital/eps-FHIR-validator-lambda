package software.nhs.fhirvalidator.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SimplifierPackage {
    @SerializedName("packageName")
    @Expose
    public String packageName;
    @SerializedName("version")
    @Expose
    public String version;
}
