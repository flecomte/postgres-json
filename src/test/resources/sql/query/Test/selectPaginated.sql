SELECT json_build_array(
   json_build_object('id', 3, 'name', :name::text),
   json_build_object('id', 4, 'name', :name::text || '-2')
), 10 as total
LIMIT :limit OFFSET :offset