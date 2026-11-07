package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.DebtDao
import com.braveboy.hesabi.data.local.dao.PersonDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.Person
import com.braveboy.hesabi.domain.model.PersonWithDebts
import com.braveboy.hesabi.domain.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PersonRepositoryImpl(
    private val personDao: PersonDao,
    private val debtDao: DebtDao,
) : PersonRepository {

    override fun observeAll(): Flow<List<Person>> =
        personDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeWithDebts(): Flow<List<PersonWithDebts>> =
        combine(personDao.observeAll(), debtDao.observeAll()) { people, debts ->
            val debtsByPerson = debts.groupBy { it.personId }
            people.map { personEntity ->
                val person = personEntity.toDomain()
                val personDebts = debtsByPerson[person.id].orEmpty().map { it.toDomain() }
                PersonWithDebts(person = person, debts = personDebts)
            }
        }

    override suspend fun getAll(): List<Person> =
        personDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Person? =
        personDao.findById(id)?.toDomain()

    override suspend fun save(person: Person) =
        personDao.upsert(person.toEntity())

    override suspend fun delete(id: String) {
        debtDao.deleteByPersonId(id)
        personDao.deleteById(id)
    }
}
