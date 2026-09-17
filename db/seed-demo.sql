insert into restaurants (id, name, restaurant_type)
values ('00000000-0000-0000-0000-000000000001', 'Harbor Spoon Cafe', 'Casual dining');

insert into users (id, email, owner_name, password_hash)
values (
    '00000000-0000-0000-0000-000000000201',
    'owner@harborspoon.test',
    'Sam Rivera',
    '$2a$10$replace-with-bcrypt-hash-from-application-bootstrap'
);

insert into restaurant_memberships (id, restaurant_id, user_id, role)
values (
    '00000000-0000-0000-0000-000000000301',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000201',
    'owner'
);

insert into vendors (id, restaurant_id, display_name, normalized_name, default_category)
values
('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001', 'Toast POS Deposit', 'toast pos deposit', 'Revenue'),
('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000001', 'Sysco', 'sysco', 'Food & beverage'),
('00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000001', 'Restaurant Depot', 'restaurant depot', 'Food & beverage'),
('00000000-0000-0000-0000-000000000104', '00000000-0000-0000-0000-000000000001', 'Toast Payroll', 'toast payroll', 'Labor'),
('00000000-0000-0000-0000-000000000105', '00000000-0000-0000-0000-000000000001', 'Main Street Properties', 'main street properties', 'Rent');
