package raff.stein.customer.model.bo.customer.enumeration;

public enum CustomerUpdateFieldsDomain {
    BASE,               //name, email, phone, address, ecc...
    FINANCIALS,         //financials related fields
    GOALS,              //goals related fields
    MIFID,              //MIFID related fields
    AML,                //AML related fields
    DOCUMENTS,          //documents related fields
    ALL;                //all fields
}
