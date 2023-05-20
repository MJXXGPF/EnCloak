
#include <stdio.h>
#include <stdlib.h>


using namespace std;

template<class Key, class Value>
class HashNode
{
public:
	Key    _key;
	Value  _value;
	HashNode *next;

	HashNode(Key key, Value value)
	{
		_key = key;
		_value = value;
		next = nullptr;
	}
	~HashNode()
	{

	}
	HashNode& operator=(const HashNode& node)
	{
		_key = node.key;
		_value = node.key;
		next = node.next;
		return *this;
	}
};

template <class Key, class Value, class HashFunc, class EqualKey>
class HashMap
{
public:
	HashMap(int size);
	~HashMap();
	bool insert(const Key& key, const Value& value);
	bool del(const Key& key);
	Value& find(const Key& key);
	Value& operator [](const Key& key);

private:
	HashFunc hash;
	EqualKey equal;
	HashNode<Key, Value> **table;
	unsigned int _size;
	Value ValueNULL;
};


template <class Key, class Value, class HashFunc, class EqualKey>
HashMap<Key, Value, HashFunc, EqualKey>::HashMap(int size) : _size(size)
{
	hash = HashFunc();
	equal = EqualKey();
	table = new HashNode<Key, Value> *[_size];
	for (unsigned i = 0; i < _size; i++)
		table[i] = nullptr;
}



template <class Key, class Value, class HashFunc, class EqualKey>
HashMap<Key, Value, HashFunc, EqualKey>::~HashMap()
{
	for (unsigned i = 0; i < _size; i++)
	{
		HashNode<Key, Value> *currentNode = table[i];
		while (currentNode)
		{
			HashNode<Key, Value> *temp = currentNode;
			currentNode = currentNode->next;
			delete temp;
		}
	}
	delete table;
}


template <class Key, class Value, class HashFunc, class EqualKey>
bool HashMap<Key, Value, HashFunc, EqualKey>::insert(const Key& key, const Value& value)
{
	int index = hash(key) % _size;
	HashNode<Key, Value> *node = new HashNode<Key, Value>(key, value);
	node->next = table[index];
	table[index] = node;
	return true;
}
template <class Key, class Value, class HashFunc, class EqualKey>
bool HashMap<Key, Value, HashFunc, EqualKey>::del(const Key& key)
{
	unsigned index = hash(key) % _size;
	HashNode<Key, Value> * node = table[index];
	HashNode<Key, Value> * prev = nullptr;
	while (node)
	{
		if (node->_key == key)
		{
			if (prev == nullptr)
			{
				table[index] = node->next;
			}
			else
			{
				prev->next = node->next;
			}
			delete node;
			return true;
		}
		prev = node;
		node = node->next;
	}
	return false;
}


template <class Key, class Value, class HashFunc, class EqualKey>
Value& HashMap<Key, Value, HashFunc, EqualKey>::find(const Key& key)
{
	unsigned  index = hash(key) % _size;
	if (table[index] == nullptr)
		return ValueNULL;
	else
	{
		HashNode<Key, Value> * node = table[index];
		while (node)
		{
			
			if (node->_key == key)
				return node->_value;
			node = node->next;
		}
		return ValueNULL;
	}
}


template <class Key, class Value, class HashFunc, class EqualKey>
Value& HashMap<Key, Value, HashFunc, EqualKey>::operator [](const Key& key)
{
	return find(key);
}
