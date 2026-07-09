package com.smithware.tidypilot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HouseholdSharingFieldsTest {
    @Test
    fun householdFieldsAreOptionalByDefault() {
        val task = CleaningTaskEntity(name = "Wipe counter", roomId = "kitchen")
        val completion = TaskCompletionEntity(taskId = task.id)

        assertNull(task.assignedTo)
        assertNull(task.householdId)
        assertNull(task.createdBy)
        assertNull(completion.householdId)
        assertNull(completion.completedBy)
    }

    @Test
    fun localAssignmentCanBeCopiedToCompletionMetadata() {
        val task = CleaningTaskEntity(
            name = "Take out trash",
            roomId = "kitchen",
            assignedTo = "Alex",
            householdId = "local-household"
        )
        val completion = TaskCompletionEntity(
            taskId = task.id,
            householdId = task.householdId,
            completedBy = task.assignedTo
        )

        assertEquals("local-household", completion.householdId)
        assertEquals("Alex", completion.completedBy)
    }
}
