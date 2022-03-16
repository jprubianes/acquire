package pages;

import com.friskysoft.framework.Element;


public class Departments {

    public Element settings = new Element(".icon-setting");

    public Element accountSettings = new Element(".icon-account-setting");

    public Element departments = new Element("[href='/settings/departments'] .menu-link-text");

    public Element emptyDepartmentTitle = new Element(".empty-content__title");

    public Element readDocsButton = new Element(".is-simple");

    public Element deleteButtonConfirmation = new Element("#delete1");


    //New department entry modal
    public Element createNewDepartmentEmptyData = new Element(".button.is-primary");

    public Element inputDepartmentNameText = new Element("#department-name1");

    public Element inputDepartmentStatus = new Element(".dropdown-components");

    public Element addUser = new Element(".add-department-user-btn-section");

    public Element cancelButton = new Element("#cancel0");

    public Element saveButton = new Element("#save1");

    public Element userList = new Element(".user-department-user-item-list");

    public Element searchUserList = new Element("#search-component-375");


    //Department table section
    public Element departmentTableList = new Element("#department-user-table-list");

    public Element departmentNameTableColumn = new Element(".setting-table-main .td__2");

    public Element popoverMenuDepartmentOption = new Element(".setting-table-main .td__5");

    public Element popoverDelete = new Element("button .icon-delete");

    public Element addDepartmentButtonExistingData = new Element(".setting-add-new-que");


}
