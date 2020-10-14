package be.kuritsu.gt.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Builder
@DynamoDBTable(tableName = "Purchase")
public class PurchaseEntity {

    private String id;
    private String purchaseDate;
    private String brand;
    private Set<String> descriptionTags;
    private BigDecimal unitPrice;
    private String locationId;
    private String locationDescription;
    private String locationLocationTag;

    /*
     Tried to have 2 entities extends an abstract one that contains the property definitions so that test data could be
     stored in a dedicated table but didn't work. Another "datasource" would require another AWS account. Maybe the group
     feature in DynamoDB could help but didn't explore that yet...
     So for the time being, I'm flagging test data so that I could clean them up after test executions (OK, not very
     elegant for a workaround but couldn't find any better solution right now, ...)
     */
    private boolean testData;

    @DynamoDBHashKey
    @DynamoDBAutoGeneratedKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDBAttribute
    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    @DynamoDBAttribute
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    @DynamoDBAttribute
    public Set<String> getDescriptionTags() {
        return descriptionTags;
    }

    public void setDescriptionTags(Set<String> descriptionTags) {
        this.descriptionTags = descriptionTags;
    }

    @DynamoDBAttribute
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "locationIdx")
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    @DynamoDBAttribute
    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    @DynamoDBAttribute
    public String getLocationLocationTag() {
        return locationLocationTag;
    }

    public void setLocationLocationTag(String locationLocationTag) {
        this.locationLocationTag = locationLocationTag;
    }

    @DynamoDBAttribute
    public boolean getTestData() {
        return testData;
    }

    public void setTestData(boolean testData) {
        this.testData = testData;
    }
}
