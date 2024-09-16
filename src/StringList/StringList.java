package StringList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringList implements List<Object> {
    private String data;
    private int size;
    private ArrayList<Integer> strides;

    public StringList() {
        this.data = new String();
        this.size = 0;
        this.strides = new ArrayList<Integer>();
    }

    public StringList(String data, int size, ArrayList<Integer> strides) {
        this.data = data;
        this.size = size;
        this.strides = strides;
    }

    public StringList(Collection<Object> data) {
        this.size = data.size();
        this.strides = new ArrayList<Integer>();
        this.addAll(data);
    }

    static byte[] serialize(Object o) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
            oos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String bytesToString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    static byte[] stringToBytes(String str) {
        return Base64.getDecoder().decode(str);
    }

    static Object deserialize(byte[] bytes) {
        InputStream is = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public String stringData() {
        return this.data;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return this.indexOf(o) != -1;
    }

    @Override
    public Iterator<Object> iterator() {
        return new StringListIterator();
    }

    public class StringListIterator implements Iterator<Object> {
        private int position;
        private int last_returned;

        public StringListIterator() {
            this.position = 0;
            this.last_returned = -1;
        }

        public StringListIterator(int index) {
            this.position = index;
            this.last_returned = -1;
        }

        @Override
        public boolean hasNext() {
            return this.position != StringList.this.size;
        }

        @Override
        public Object next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.position++;
            this.last_returned = this.position;
            Object current_value = StringList.this.get(this.position);
            return current_value;
        }

        @Override
        public void remove() {
            if (this.last_returned != -1) {
                throw new IllegalStateException();
            }

            StringList.this.remove(this.last_returned);
            this.position = this.last_returned;
            this.last_returned = -1;
        }
    }

    @Override
    public Object[] toArray() {
        byte[] all_bytes = stringToBytes(this.data);

        Object[] objects = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            int obj_size = this.strides.get(i);
            byte[] obj_bytes = new byte[obj_size];
            System.arraycopy(all_bytes, i * obj_size, obj_size, 0, obj_size);

            objects[i] = deserialize(obj_bytes);
        }

        return objects;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E[] toArray(E[] a) {
        if (a.length < this.size) {
            return (E[]) Arrays.copyOf(this.toArray(), size, a.getClass());
        }
        System.arraycopy(this.toArray(), 0, a, 0, this.size);
        return a;
    }

    @Override
    public boolean add(Object e) {
        this.add(this.size, e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        int obj_index = this.indexOf(o);
        if (obj_index == -1) {
            return false;
        }
        this.remove(obj_index);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object item : c) {
            if (!this.contains(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<?> c) {
        return this.addAll(this.size, c);
    }

    @Override
    public boolean addAll(int index, Collection<?> c) {
        if (index < 0 || index > this.size) {
            throw new IndexOutOfBoundsException();
        }

        byte[] all_bytes = stringToBytes(this.data);

        Object[] c_arr = c.toArray();
        int[] obj_sizes = new int[c_arr.length];

        if (c.isEmpty()) {
            return false;
        }

        for (int i = 0; i < c_arr.length; i++) {
            obj_sizes[i] = serialize(c_arr[i]).length;
        }

        int added_size = IntStream.of(obj_sizes).sum();
        byte[] new_bytes = new byte[all_bytes.length + added_size];

        int prefix_size = 0;
        for (int i = 0; i < index; i++) {
            int current_stride = this.strides.get(i);
            System.arraycopy(all_bytes, prefix_size, new_bytes, prefix_size, current_stride);
            prefix_size += current_stride;
        }

        int middle_size = 0;
        for (int i = 0; i < c_arr.length; i++) {
            byte[] object_bytes = serialize(c_arr[i]);
            int byte_count = object_bytes.length;
            System.arraycopy(object_bytes, 0, new_bytes, prefix_size + middle_size, byte_count);
            middle_size += byte_count;
        }

        int suffix_size = 0;
        for (int i = index; i < this.size; i++) {
            int current_stride = this.strides.get(i);
            System.arraycopy(all_bytes, prefix_size + suffix_size, new_bytes, prefix_size + middle_size + suffix_size,
                    current_stride);
            suffix_size += current_stride;
        }

        this.data = bytesToString(new_bytes);
        this.size += c.size();
        Collection<Integer> new_strides = Arrays.stream(obj_sizes).boxed().collect(Collectors.toList());
        this.strides.addAll(index, new_strides);

        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        int prev_size = this.size;
        for (Object item : c) {
            this.remove(item);
        }

        return this.size != prev_size;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        int prev_size = this.size;
        for (int i = prev_size - 1; i >= 0; i--) {
            Object current_obj = this.get(i);
            if (!c.contains(current_obj)) {
                this.remove(i);
            }
        }
        return this.size != prev_size;
    }

    @Override
    public void clear() {
        this.data = new String();
        this.size = 0;
        this.strides = new ArrayList<Integer>();
    }

    @Override
    public Object get(int index) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException();
        }
        byte[] all_bytes = stringToBytes(this.data);
        int current_stride = this.strides.get(index);
        byte[] selected_bytes = new byte[current_stride];

        int padding = 0;
        for (int i = 0; i < index; i++) {
            padding += this.strides.get(i);
        }

        System.arraycopy(all_bytes, padding, selected_bytes, 0, current_stride);
        return deserialize(selected_bytes);
    }

    @Override
    public Object set(int index, Object element) {
        byte[] all_bytes = stringToBytes(this.data);
        int old_element_stride = this.strides.get(index);
        byte[] prev_bytes = new byte[old_element_stride];

        int padding = 0;
        for (int i = 0; i < index; i++) {
            padding += this.strides.get(i);
        }

        System.arraycopy(all_bytes, padding, prev_bytes, 0, old_element_stride);

        byte[] element_bytes = serialize(element);
        int new_stride = element_bytes.length;
        int new_byte_count = all_bytes.length - old_element_stride + new_stride;
        byte[] new_bytes = new byte[new_byte_count];

        System.arraycopy(all_bytes, 0, new_bytes, 0, padding);
        System.arraycopy(element_bytes, 0, new_bytes, padding, new_stride);
        System.arraycopy(all_bytes, padding + old_element_stride, new_bytes, padding + new_stride,
                new_byte_count - padding - new_stride);

        this.data = bytesToString(new_bytes);
        this.strides.set(index, new_stride);

        return deserialize(prev_bytes);
    }

    @Override
    public void add(int index, Object element) {
        Collection<Object> temp = Arrays.asList(new Object[]{element});
        this.addAll(index, temp);
    }

    @Override
    public Object remove(int index) {
        byte[] all_bytes = stringToBytes(this.data);

        int object_size = this.strides.get(index);

        int byte_count = all_bytes.length - object_size;
        byte[] new_bytes = new byte[byte_count];
        byte[] removed_bytes = new byte[object_size];

        int padding = 0;
        for (int i = 0; i < index; i++) {
            padding += this.strides.get(i);
        }

        System.arraycopy(all_bytes, 0, new_bytes, 0, padding);
        System.arraycopy(all_bytes, padding, removed_bytes, 0, object_size);
        System.arraycopy(all_bytes, padding + object_size, new_bytes, padding, byte_count - padding);

        this.data = bytesToString(new_bytes);
        this.size--;

        this.strides.remove(index);

        return deserialize(removed_bytes);
    }

    @Override
    public int indexOf(Object o) {
        byte[] serialized_obj = serialize(o);
        byte[] all_bytes = stringToBytes(this.data);

        int padding = 0;
        for (int i = 0; i < this.size; i++) {
            int current_obj_size = this.strides.get(i);
            byte[] current_obj_bytes = new byte[current_obj_size];
            System.arraycopy(all_bytes, padding, current_obj_bytes, 0, current_obj_size);
            if (current_obj_bytes.equals(serialized_obj)) {
                return i;
            }
            padding += current_obj_size;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        byte[] serialized_obj = serialize(o);
        byte[] all_bytes = stringToBytes(this.data);

        int padding = 0;
        for (int i = this.size - 1; i >= 0; i--) {
            int current_obj_size = this.strides.get(i);
            byte[] current_obj_bytes = new byte[current_obj_size];
            System.arraycopy(all_bytes, padding, current_obj_bytes, 0, current_obj_size);
            if (current_obj_bytes.equals(serialized_obj)) {
                return i;
            }
            padding += current_obj_size;
        }
        return -1;
    }

    @Override
    public ListIterator<Object> listIterator() {
        return this.listIterator(0);
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        return new StringListListIterator(index);
    }

    public class StringListListIterator extends StringListIterator implements ListIterator<Object> {
        private int position;
        private int last_returned;

        public StringListListIterator(int index) {
            super(index);
        }

        @Override
        public boolean hasPrevious() {
            return position > 0;
        }

        @Override
        public Object previous() {
            if (!this.hasPrevious()) {
                throw new NoSuchElementException();
            }
            this.position--;
            this.last_returned = this.position;
            Object item = StringList.this.get(this.position);
            return item;
        }

        @Override
        public int nextIndex() {
            return this.position;
        }

        @Override
        public int previousIndex() {
            return this.position - 1;
        }

        @Override
        public void set(Object e) {
            if (this.last_returned == -1) {
                throw new IllegalStateException();
            }

            StringList.this.set(this.last_returned, e);
            this.position = this.last_returned;
            this.last_returned = -1;
        }

        @Override
        public void add(Object e) {
            StringList.this.add(this.position, e);
            this.position++;
            this.last_returned = -1;
        }

    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        byte[] all_bytes = stringToBytes(this.data);
        int new_size = toIndex - fromIndex;

        int padding = 0;
        int sublist_size = 0;
        for (int i = 0; i < toIndex; i++) {
            if (i < fromIndex) {
                padding += this.strides.get(i);
            } else {
                sublist_size += this.strides.get(i);
            }
        }
        byte[] sublist_bytes = new byte[sublist_size];

        System.arraycopy(all_bytes, padding, sublist_bytes, 0, sublist_size);

        return new StringList(bytesToString(sublist_bytes), new_size, this.strides);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("[");

        for (int i = 0; i < this.size; i++) {
            res.append(this.get(i).toString());

            if (i < this.size - 1) {
                res.append(", ");
            }
        }
        res.append("]");

        return res.toString();
    }

    @Override
    public boolean equals(Object o) {
        StringList cast_o = (StringList) o;
        if (this == o) {
            return true;
        }

        if (this.data.equals(cast_o.data) && this.size == cast_o.size && this.strides == cast_o.strides) {
            return true;
        }

        return false;
    }
}