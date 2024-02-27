package software.nhs.FHIRValidator.models;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class IgIndex {
    @SerializedName("index-version")
    @Expose
    public Integer indexVersion;
    @SerializedName("files")
    @Expose
    public List<IgFile> files = null;
}
