package crmdna.common.contact;

public class PostalAddressProp {
    public String address;
    public String city;
    public String state;
    public String country;
    public String pincode;

    String getFullAddress() {
        StringBuilder builder = new StringBuilder();

        if (address != null)
            builder.append(address + ", ");

        if (city != null)
            builder.append(city + ", ");

        if (state != null)
            builder.append(state + ", ");

        if (country != null)
            builder.append(country + ", ");

        if (pincode != null)
            builder.append("Pincode " + pincode);

        return builder.toString();
    }
}
